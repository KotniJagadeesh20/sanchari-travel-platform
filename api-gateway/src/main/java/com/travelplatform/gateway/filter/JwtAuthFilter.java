package com.travelplatform.gateway.filter;

import com.travelplatform.gateway.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.regex.Pattern;

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
     * Paths that bypass JWT validation at the gateway ENTIRELY — no
     * Authorization header required, and none forwarded even if present.
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

    /**
     * Browsing — search/list/details for rides, packages, and destinations —
     * is public, matching Bus and Hotel: anyone can look, only
     * booking/creating/managing needs an account. Unlike PUBLIC_PATHS above,
     * a JWT here is OPTIONAL rather than stripped: if the caller does have a
     * valid token, we still forward identity headers (e.g. RideDetails.tsx
     * uses X-Authenticated-User-Id-derived identity client-side to compute
     * "is this my own ride"), we just don't reject the request when there
     * isn't one.
     *
     * Deliberately narrow, not a blanket prefix match: a plain "/rides/*" or
     * "/packages/*" would also expose GET /rides/driver, GET /rides/bookings,
     * and GET /packages/bookings — a driver's own rides / a passenger's own
     * bookings — which must stay authenticated. Those sit at the exact same
     * single-path-segment depth as /rides/{rideId} and /packages/{packageId},
     * so the only reliable way to tell "an ID" apart from "a reserved word"
     * here is the UUID format — the *_ID_PATTERN constants below. Destinations
     * has no such collision (no "my destinations" concept), so its rule is
     * simpler: everything under /destinations/ is public except
     * /destinations/admin/**.
     *
     * Keep these patterns in sync with the equivalent SecurityConfig in
     * ride-share-service and travel-packages-service (defense in depth —
     * both layers enforce the same rule independently).
     */
    private static final Pattern RIDE_ID_PATTERN =
            Pattern.compile("^/rides/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private static final Pattern PACKAGE_ID_PATTERN =
            Pattern.compile("^/packages/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private static final Pattern DESTINATION_ID_PATTERN =
            Pattern.compile("^/destinations/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private boolean isPublicBrowseRead(HttpMethod method, String path) {
        if (method != HttpMethod.GET) return false;

        if (path.equals("/rides/search") || RIDE_ID_PATTERN.matcher(path).matches()) return true;

        if (path.equals("/packages") || path.startsWith("/packages/by-destination/")
                || PACKAGE_ID_PATTERN.matcher(path).matches()) return true;

        if (path.startsWith("/destinations/admin")) return false; // must stay authenticated — checked first
        if (path.equals("/destinations") || path.equals("/destinations/search")
                || path.equals("/destinations/popular") || path.startsWith("/destinations/category/")
                || DESTINATION_ID_PATTERN.matcher(path).matches()) return true;

        return false;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        HttpMethod method = exchange.getRequest().getMethod();

        if (PUBLIC_PATHS.stream().anyMatch(path::startsWith)) return chain.filter(exchange);

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        boolean hasBearerToken = authHeader != null && authHeader.startsWith("Bearer ");

        if (!hasBearerToken) {
            if (isPublicBrowseRead(method, path)) return chain.filter(exchange);
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
            // A public browse read with a present-but-invalid/expired token:
            // don't block browsing over a stale credential — treat as a guest.
            if (isPublicBrowseRead(method, path)) {
                log.info("Ignoring invalid token on public read {} — continuing as guest: {}", path, e.getMessage());
                return chain.filter(exchange);
            }
            log.warn("JWT validation failed for {}: {}", path, e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() { return -1; }
}
