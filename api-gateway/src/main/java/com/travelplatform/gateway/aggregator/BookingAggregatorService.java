package com.travelplatform.gateway.aggregator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * The Booking Aggregator. NOT a booking service — owns no data, no
 * persistence, no booking business logic. Its only job is to call the four
 * existing "my bookings" endpoints in parallel and merge whatever they
 * return. Each source is independently timed-out and circuit-broken
 * (Resilience4j, configured in application.yml) so one slow/down service
 * degrades that one field to [] + a warning instead of failing the whole
 * request — see the Mono.zip in aggregate().
 *
 * Adding a future source (e.g. for /me/trips) means: one more WebClient
 * field, one more fetch method following the fetchArray() pattern below,
 * one more resilience4j.instances entry in application.yml, and one more
 * slot in whatever response DTO that new endpoint returns. Nothing here
 * needs to change.
 */
@Service
public class BookingAggregatorService {

    private static final Logger log = LoggerFactory.getLogger(BookingAggregatorService.class);

    private final WebClient busClient;
    private final WebClient rideClient;
    private final WebClient hotelClient;
    private final WebClient packageClient;
    private final ReactiveResilience4JCircuitBreakerFactory circuitBreakerFactory;
    private final ObjectMapper objectMapper;

    public BookingAggregatorService(WebClient.Builder loadBalancedWebClientBuilder,
                                     ReactiveResilience4JCircuitBreakerFactory circuitBreakerFactory,
                                     ObjectMapper objectMapper) {
        this.busClient = loadBalancedWebClientBuilder.clone().baseUrl("lb://bus-booking-service").build();
        this.rideClient = loadBalancedWebClientBuilder.clone().baseUrl("lb://ride-share-service").build();
        this.hotelClient = loadBalancedWebClientBuilder.clone().baseUrl("lb://hotel-service").build();
        this.packageClient = loadBalancedWebClientBuilder.clone().baseUrl("lb://travel-packages-service").build();
        this.circuitBreakerFactory = circuitBreakerFactory;
        this.objectMapper = objectMapper;
    }

    public Mono<AggregatedBookingsResponse> aggregate(String userId) {
        // Mono.zip subscribes to all four immediately — genuinely parallel,
        // not sequential. None of these four Monos can emit an error: every
        // failure path is already converted to a fallback SourceResult
        // inside fetchBus()/fetchArray(), so zip can't short-circuit on one
        // source's failure and lose the others.
        return Mono.zip(
                fetchBus(userId),
                fetchArray(rideClient, "/rides/bookings", userId, "rideBookings", "Ride bookings"),
                fetchArray(hotelClient, "/hotel-bookings/me", userId, "hotelBookings", "Hotel bookings"),
                fetchArray(packageClient, "/packages/bookings", userId, "packageBookings", "Package bookings")
        ).map(tuple -> {
            AggregatedBookingsResponse response = new AggregatedBookingsResponse();
            response.setBusBookings(tuple.getT1().data());
            response.setRideBookings(tuple.getT2().data());
            response.setHotelBookings(tuple.getT3().data());
            response.setPackageBookings(tuple.getT4().data());

            List<String> warnings = Stream.of(tuple.getT1(), tuple.getT2(), tuple.getT3(), tuple.getT4())
                    .map(SourceResult::warning)
                    .filter(Objects::nonNull)
                    .toList();
            response.setWarnings(warnings.isEmpty() ? null : warnings);
            return response;
        });
    }

    /**
     * Bus is the one source with a quirk the other three don't have:
     * bus-booking-service's GET /api/user/bookingDetails returns 404 with
     * {success:false, message:"No bookings found for user"} when the user
     * simply has zero bookings — that's a legitimate empty result, NOT a
     * failure, and must not trip the circuit breaker or produce a warning.
     * Only genuine errors (5xx, timeout, connection failure) should. So the
     * 404 case is handled inside the primary Mono (still "success"), and
     * only real failures reach the circuit breaker's fallback.
     */
    private Mono<SourceResult> fetchBus(String userId) {
        Mono<SourceResult> call = busClient.get()
                .uri("/api/user/bookingDetails")
                .header("X-Authenticated-User-Id", userId)
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return response.bodyToMono(JsonNode.class)
                                .map(body -> new SourceResult("busBookings", body.path("bookingDetails"), null));
                    }
                    if (response.statusCode() == HttpStatus.NOT_FOUND) {
                        return Mono.just(new SourceResult("busBookings", objectMapper.createArrayNode(), null));
                    }
                    return response.createException().flatMap(Mono::error);
                });

        ReactiveCircuitBreaker cb = circuitBreakerFactory.create("busBookings");
        return cb.run(call, throwable -> {
            log.warn("Bus booking aggregation failed: {}", throwable.toString());
            return Mono.just(new SourceResult("busBookings", objectMapper.createArrayNode(), "Bus bookings are temporarily unavailable."));
        });
    }

    /**
     * Ride, hotel, and package's "my bookings" endpoints all share the same
     * shape: 200 with a JSON array, empty array (not 404) when the user has
     * none. So any non-2xx here is a genuine failure.
     */
    private Mono<SourceResult> fetchArray(WebClient client, String path, String userId, String key, String label) {
        Mono<SourceResult> call = client.get()
                .uri(path)
                .header("X-Authenticated-User-Id", userId)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(body -> new SourceResult(key, body, null));

        ReactiveCircuitBreaker cb = circuitBreakerFactory.create(key);
        return cb.run(call, throwable -> {
            log.warn("{} aggregation failed: {}", key, throwable.toString());
            return Mono.just(new SourceResult(key, objectMapper.createArrayNode(), label + " are temporarily unavailable."));
        });
    }
}
