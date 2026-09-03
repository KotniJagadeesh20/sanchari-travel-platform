package com.travelplatform.gateway.aggregator;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Booking Aggregator (V1) — handled directly by the gateway app, not proxied
 * to a downstream service. Not routed through application.yml's `routes:`
 * list on purpose: this endpoint's job is to fan out to FOUR services and
 * merge results, which a simple proxy Route can't do.
 *
 * Deliberately under /me/** rather than /api/me/** as originally sketched:
 * bus-booking-service's route already claims the whole /api/** prefix
 * (Path=/api/**, /admin/**), so /api/me/bookings would collide with that
 * route. /me/** doesn't overlap any existing route predicate.
 *
 * Auth: LocalAuthFilter (a plain WebFilter, not a Gateway GlobalFilter —
 * see its Javadoc for why) validates the JWT for every /me/** request
 * before this controller ever sees it, and sets the same
 * X-Authenticated-User-Id header downstream services already read. A
 * missing/invalid JWT gets a 401 from that filter — this controller doesn't
 * need to re-check.
 */
@RestController
@RequestMapping("/me")
public class BookingAggregatorController {

    private final BookingAggregatorService aggregatorService;

    public BookingAggregatorController(BookingAggregatorService aggregatorService) {
        this.aggregatorService = aggregatorService;
    }

    /**
     * All of the caller's bookings across bus, ride, hotel, and package
     * services — four downstream calls in parallel. Never fails because one
     * service is down: that source comes back as an empty array plus an
     * entry in `warnings`.
     */
    @GetMapping("/bookings")
    public Mono<AggregatedBookingsResponse> getMyBookings(@RequestHeader("X-Authenticated-User-Id") String userId) {
        return aggregatorService.aggregate(userId);
    }
}
