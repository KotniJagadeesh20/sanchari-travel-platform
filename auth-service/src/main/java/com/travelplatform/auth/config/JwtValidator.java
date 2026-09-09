package com.travelplatform.auth.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtValidator extends OncePerRequestFilter{

	private final SecretKey key;

	public JwtValidator(String jwtSecret) {
		this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
			String jwt = request.getHeader(JwtConstant.JWT_HEADER);
			
			if (jwt != null) {
				try {
					if (!jwt.startsWith("Bearer ") || jwt.length() <= 7) {
						throw new BadCredentialsException("Malformed Authorization header");
					}
					jwt = jwt.substring(7);
					Claims claims = Jwts.parserBuilder().setSigningKey(key).build()
							.parseClaimsJws(jwt).getBody();
					
					String email = String.valueOf(claims.get("email"));
					
					Object authoritiesClaim = claims.get("authorities");
					
					String authorities = authoritiesClaim != null ? authoritiesClaim.toString() : "";
					
					List<GrantedAuthority> auths = authorities.isBlank()
							? new ArrayList<>()
							: AuthorityUtils.commaSeparatedStringToAuthorityList(authorities);
					
					Authentication authentication = new UsernamePasswordAuthenticationToken(email, null , auths);
					
					SecurityContextHolder.getContext().setAuthentication(authentication);
					
					
				}catch (Exception e) {
					throw new BadCredentialsException("Invalid Token.. from JWT Validator");
				}
			}
	
			
			filterChain.doFilter(request, response);
			
	}

}
