package com.travelplatform.hotel.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Populates the SecurityContext from identity headers forwarded by the API Gateway
 * (X-Authenticated-Email, X-Authenticated-Authorities), rather than re-parsing the JWT.
 * Mirrors travel-packages-service / bus-booking-service's JwtValidator — relies on the
 * same network-isolation requirement (this service must not be reachable except through
 * the gateway, or the headers could be spoofed).
 */
public class JwtValidator extends OncePerRequestFilter {

    private static final String EMAIL_HEADER = "X-Authenticated-Email";
    private static final String AUTHORITIES_HEADER = "X-Authenticated-Authorities";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String email = request.getHeader(EMAIL_HEADER);

        if (email != null && !email.isBlank()) {
            String authoritiesHeader = request.getHeader(AUTHORITIES_HEADER);
            String authorities = authoritiesHeader != null ? authoritiesHeader : "";

            List<GrantedAuthority> auths = authorities.isBlank()
                    ? new ArrayList<>()
                    : AuthorityUtils.commaSeparatedStringToAuthorityList(authorities);

            Authentication authentication = new UsernamePasswordAuthenticationToken(email, null, auths);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
