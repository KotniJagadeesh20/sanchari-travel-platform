package com.travelplatform.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.List;

/**
 * Validates JWT on every non-public request and forwards identity headers downstream.
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

    @Value("${jwt.secret}")
    private String jwtSecret;

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
            String token = authHeader.substring(7);
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            Claims claims = Jwts.parserBuilder().setSigningKey(key).build()
                    .parseClaimsJws(token).getBody();

            String email = String.valueOf(claims.get("email"));
            String name = claims.get("name") != null
                    ? String.valueOf(claims.get("name")) : "";
            String authorities = claims.get("authorities") != null
                    ? String.valueOf(claims.get("authorities")) : "";
            String userId = claims.get("userId") != null
                    ? String.valueOf(claims.get("userId")) : "";

            ServerHttpRequest mutated = exchange.getRequest().mutate()
                    .header("X-Authenticated-Email", email)
                    .header("X-Authenticated-Name", name)
                    .header("X-Authenticated-Authorities", authorities)
                    .header("X-Authenticated-User-Id", userId)
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
