package com.travelplatform.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

/**
 * Shared JWT parsing, used by both JwtAuthFilter (GlobalFilter — validates
 * requests proxied to downstream services via a Gateway Route) and
 * LocalAuthFilter (plain WebFilter — validates requests handled locally by
 * this gateway app, like the booking aggregator, which never go through a
 * Route and so never see GlobalFilter). Extracted here so the two filters
 * don't duplicate the HMAC-parsing logic.
 */
@Component
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    public record AuthenticatedUser(String userId, String email, String name, String authorities) {}

    /** @throws JwtException if the token is missing, malformed, expired, or has a bad signature. */
    public AuthenticatedUser parse(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody();

        String email = String.valueOf(claims.get("email"));
        String name = claims.get("name") != null ? String.valueOf(claims.get("name")) : "";
        String authorities = claims.get("authorities") != null ? String.valueOf(claims.get("authorities")) : "";
        String userId = claims.get("userId") != null ? String.valueOf(claims.get("userId")) : "";
        return new AuthenticatedUser(userId, email, name, authorities);
    }
}
