package com.travelplatform.notification.config;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Guards /internal/** — the service-to-service notification-creation endpoint that
 * is deliberately NOT routed through the API Gateway (see api-gateway's route
 * config: only /notifications/** is exposed, not /internal/**). Any container that
 * can reach notification-service on the Docker network can still call this path
 * directly, so it's protected with a shared secret rather than left wide open.
 *
 * This is a stand-in for real service-to-service auth (mTLS, a service mesh, or
 * short-lived service JWTs) — good enough for a single-Docker-network deployment,
 * not something to trust across an untrusted network.
 */
public class InternalApiKeyFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-Internal-Api-Key";

    private final String expectedApiKey;

    public InternalApiKeyFilter(String expectedApiKey) {
        this.expectedApiKey = expectedApiKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (request.getRequestURI().startsWith("/internal/")) {
            String providedKey = request.getHeader(API_KEY_HEADER);
            if (providedKey == null || !providedKey.equals(expectedApiKey)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Missing or invalid " + API_KEY_HEADER);
                return;
            }
            // Grant a synthetic internal-service principal so downstream authorizeHttpRequests
            // rules (hasAuthority("ROLE_INTERNAL_SERVICE")) have something to check.
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            "internal-service", null,
                            AuthorityUtils.createAuthorityList("ROLE_INTERNAL_SERVICE")));
        }

        filterChain.doFilter(request, response);
    }
}
