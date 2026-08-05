package com.travelplatform.gateway.filter;

import com.travelplatform.gateway.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Validates JWT on every non-public request PROXIED to a downstream service
 * (i.e. matched by a Route in application.yml) and forwards identity headers
 * downstream. This only runs for requests Spring Cloud Gateway routes
 * onward — locally-handled endpoints (like /me/** — see LocalAuthFilter)
 * never enter the Gateway's route-filter chain, so they need their own
 * auth filter even though the JWT-parsing logic is shared via JwtService.
 *
 * Headers added for downstream services:
 *   X-Authenticated-Email        — user's email address
 *   X-Authenticated-Name         — user's display name
 *   X-Authenticated-Authorities  — comma-separated roles (e.g. ROLE_USER)
 *   X-Authenticated-User-Id      — user's UUID (from "userId" JWT claim)
 */
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    @Autowired
    private JwtService jwtService;

    /**
     * Paths that bypass JWT validation at the gateway.
     * NOTE: /auth/users/** is intentionally NOT in this list — user profile
     * lookups require a valid JWT. Only the credential-exchange endpoints
     * (register, login, refresh, logout) are truly public.
     */
    private static final List<String> PUBLIC_PATHS = List.of(
            "/auth/userRegister",
            "/auth/registerAdmin",
            "/auth/Loginin",
            "/auth/refresh-token",
            "/auth/logout",
            "/actuator",
            "/swagger-ui",
            "/v3/api-docs"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (PUBLIC_PATHS.stream().anyMatch(path::startsWith)) return chain.filter(exchange);

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing/malformed Authorization header: {}", path);
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

    @Override
    public int getOrder() { return -1; }
}
