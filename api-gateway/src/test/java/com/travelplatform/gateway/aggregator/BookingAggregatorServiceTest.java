package com.travelplatform.gateway.aggregator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises BookingAggregatorService against a stubbed WebClient
 * ExchangeFunction — no real network calls, no Spring context needed. The
 * stub inspects request.url() (host = which downstream service, path = which
 * endpoint) to decide what canned response to return, standing in for the
 * four real "my bookings" endpoints this class calls.
 */
class BookingAggregatorServiceTest {

    private static final String USER_ID = UUID.randomUUID().toString();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private BookingAggregatorService serviceWith(ExchangeFunction exchangeFunction) {
        WebClient.Builder builder = WebClient.builder().exchangeFunction(exchangeFunction);
        // No Spring context needed — ReactiveResilience4JCircuitBreakerFactory
        // manages its own default registries when none are configured.
        return new BookingAggregatorService(builder, new ReactiveResilience4JCircuitBreakerFactory(), objectMapper);
    }

    private ClientResponse jsonResponse(HttpStatus status, String body) {
        return ClientResponse.create(status)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .build();
    }

    @Test
    void aggregate_allSourcesHealthy_returnsAllDataAndNoWarnings() {
        ExchangeFunction stub = request -> {
            String host = request.url().getHost();
            return switch (host) {
                case "bus-booking-service" -> Mono.just(jsonResponse(HttpStatus.OK,
                        "{\"success\":true,\"bookingDetails\":[{\"id\":\"bus-1\"}]}"));
                case "ride-share-service" -> Mono.just(jsonResponse(HttpStatus.OK, "[{\"id\":\"ride-1\"}]"));
                case "hotel-service" -> Mono.just(jsonResponse(HttpStatus.OK, "[{\"id\":\"hotel-1\"}]"));
                case "travel-packages-service" -> Mono.just(jsonResponse(HttpStatus.OK, "[{\"id\":\"pkg-1\"}]"));
                default -> Mono.error(new IllegalStateException("Unexpected host: " + host));
            };
        };

        StepVerifier.create(serviceWith(stub).aggregate(USER_ID))
                .assertNext(response -> {
                    assertThat(response.getBusBookings().get(0).get("id").asText()).isEqualTo("bus-1");
                    assertThat(response.getRideBookings().get(0).get("id").asText()).isEqualTo("ride-1");
                    assertThat(response.getHotelBookings().get(0).get("id").asText()).isEqualTo("hotel-1");
                    assertThat(response.getPackageBookings().get(0).get("id").asText()).isEqualTo("pkg-1");
                    assertThat(response.getWarnings()).isNull();
                })
                .verifyComplete();
    }

    /**
     * The important edge case: bus-booking-service's 404-for-no-bookings is a
     * legitimate empty result, not a failure. It must produce an empty array
     * with NO warning — not the same "temporarily unavailable" fallback a
     * real failure would produce.
     */
    @Test
    void aggregate_busHasNoBookings_returnsEmptyArrayWithoutWarning() {
        ExchangeFunction stub = request -> {
            String host = request.url().getHost();
            return switch (host) {
                case "bus-booking-service" -> Mono.just(jsonResponse(HttpStatus.NOT_FOUND,
                        "{\"success\":false,\"message\":\"No bookings found for user\"}"));
                default -> Mono.just(jsonResponse(HttpStatus.OK, "[]"));
            };
        };

        StepVerifier.create(serviceWith(stub).aggregate(USER_ID))
                .assertNext(response -> {
                    assertThat(response.getBusBookings().isArray()).isTrue();
                    assertThat(response.getBusBookings()).isEmpty();
                    assertThat(response.getWarnings()).isNull();
                })
                .verifyComplete();
    }

    @Test
    void aggregate_oneServiceDown_returnsPartialResultsWithWarningForThatSourceOnly() {
        ExchangeFunction stub = request -> {
            String host = request.url().getHost();
            return switch (host) {
                case "bus-booking-service" -> Mono.just(jsonResponse(HttpStatus.OK,
                        "{\"success\":true,\"bookingDetails\":[{\"id\":\"bus-1\"}]}"));
                case "hotel-service" -> Mono.just(jsonResponse(HttpStatus.OK, "[{\"id\":\"hotel-1\"}]"));
                case "travel-packages-service" -> Mono.just(jsonResponse(HttpStatus.OK, "[{\"id\":\"pkg-1\"}]"));
                case "ride-share-service" -> Mono.just(jsonResponse(HttpStatus.INTERNAL_SERVER_ERROR, "{}"));
                default -> Mono.error(new IllegalStateException("Unexpected host: " + host));
            };
        };

        StepVerifier.create(serviceWith(stub).aggregate(USER_ID))
                .assertNext(response -> {
                    assertThat(response.getBusBookings()).isNotEmpty();
                    assertThat(response.getHotelBookings()).isNotEmpty();
                    assertThat(response.getPackageBookings()).isNotEmpty();
                    assertThat(response.getRideBookings().isArray()).isTrue();
                    assertThat(response.getRideBookings()).isEmpty();
                    assertThat(response.getWarnings()).hasSize(1);
                    assertThat(response.getWarnings().get(0)).containsIgnoringCase("ride");
                })
                .verifyComplete();
    }

    @Test
    void aggregate_allSourcesTimeOut_returnsAllEmptyWithFourWarningsRatherThanFailing() {
        ExchangeFunction stub = request -> Mono.delay(java.time.Duration.ofSeconds(10))
                .then(Mono.just(jsonResponse(HttpStatus.OK, "[]")));

        StepVerifier.create(serviceWith(stub).aggregate(USER_ID))
                .assertNext(response -> {
                    assertThat(response.getBusBookings()).isEmpty();
                    assertThat(response.getRideBookings()).isEmpty();
                    assertThat(response.getHotelBookings()).isEmpty();
                    assertThat(response.getPackageBookings()).isEmpty();
                    assertThat(response.getWarnings()).hasSize(4);
                })
                // Default Resilience4j TimeLimiter timeout (1s, since no
                // explicit config is loaded outside Spring context) plus
                // some slack — this only needs to prove the call completes
                // instead of hanging for the full 10s delay above.
                .expectComplete()
                .verify(java.time.Duration.ofSeconds(5));
    }

    @Test
    void aggregate_callsAllFourSourcesConcurrentlyNotSequentially() {
        AtomicInteger inFlight = new AtomicInteger(0);
        AtomicInteger maxConcurrent = new AtomicInteger(0);

        ExchangeFunction stub = request -> {
            int current = inFlight.incrementAndGet();
            maxConcurrent.updateAndGet(prev -> Math.max(prev, current));
            return Mono.delay(java.time.Duration.ofMillis(200))
                    .doFinally(sig -> inFlight.decrementAndGet())
                    .then(Mono.just(jsonResponse(HttpStatus.OK, "[]")));
        };

        StepVerifier.create(serviceWith(stub).aggregate(USER_ID))
                .expectNextCount(1)
                .verifyComplete();

        // If the four calls ran sequentially, at most 1 would ever be in
        // flight at once. Parallel execution means multiple overlap.
        assertThat(maxConcurrent.get()).isGreaterThan(1);
    }
}
