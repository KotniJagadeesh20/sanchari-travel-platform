package com.travelplatform.busbooking.config;

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
 * Populates the SecurityContext from identity headers forwarded by the API Gateway,
 * rather than re-parsing the JWT.
 *
 * The gateway (JwtAuthFilter) already validated the JWT signature and expiry once.
 * Re-validating it here would mean every downstream service needs the JWT secret
 * and duplicates work on every hop. Instead this service trusts:
 *
 *   X-Authenticated-Email         — used as the principal name
 *   X-Authenticated-Authorities   — comma-separated roles, e.g. "ROLE_USER" or "ROLE_ADMIN"
 *
 * This is safe ONLY because this service is not exposed directly to the internet —
 * all traffic is expected to arrive via the gateway, which strips/overwrites any
 * client-supplied X-Authenticated-* headers before forwarding. In a real deployment,
 * enforce this with network policy (e.g. only the gateway's IP/service account can
 * reach bus-booking-service) so a client can't forge these headers directly.
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
