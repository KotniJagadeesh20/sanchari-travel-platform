package com.travelplatform.gateway.filter;

import com.travelplatform.gateway.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Validates JWT for endpoints handled LOCALLY by this gateway app — currently
 * just the booking aggregator under /me/**. This is a plain WebFilter, not a
 * GlobalFilter: GlobalFilters only run for requests that match a Route in
 * application.yml and get proxied onward (RoutePredicateHandlerMapping).
 * /me/** isn't proxied anywhere — it's handled directly by
 * BookingAggregatorController via ordinary WebFlux dispatch — so JwtAuthFilter
 * never sees it. A plain WebFilter runs for every request regardless of how
 * it's ultimately dispatched, which is what we need here.
 *
 * On success, sets the same X-Authenticated-* headers JwtAuthFilter sets for
 * proxied routes, so BookingAggregatorController can read them with a plain
 * @RequestHeader, exactly like every downstream service controller already
 * does — no separate convention for local vs. proxied auth.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LocalAuthFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(LocalAuthFilter.class);
    private static final String LOCAL_PREFIX = "/me/";

    private final JwtService jwtService;

    public LocalAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (!path.startsWith(LOCAL_PREFIX)) return chain.filter(exchange);

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try {
            JwtService.AuthenticatedUser user = jwtService.parse(authHeader.substring(7));

            ServerHttpRequest mutated = exchange.getRequest().mutate()
                    .header("X-Authenticated-Email", user.email())
                    .header("X-Authenticated-Name", user.name())
                    .header("X-Authenticated-Authorities", user.authorities())
                    .header("X-Authenticated-User-Id", user.userId())
                    .build();

            return chain.filter(exchange.mutate().request(mutated).build());

        } catch (Exception e) {
            log.warn("JWT validation failed for {}: {}", path, e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }
}
