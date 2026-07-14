package com.travelplatform.auth.config;

import java.util.Date;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.travelplatform.auth.entity.UserAdmin;
import com.travelplatform.auth.repository.UserAdminRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Autowired;

@Service
public class JwtProvider {

    @Autowired
    private UserAdminRepository userAdminRepo;

    private SecretKey key = Keys.hmacShaKeyFor(JwtConstant.SECRET_KEY.getBytes());

    /**
     * Generates a signed JWT containing:
     *   - email       : user's email (used as the principal name)
     *   - name        : user's display name — forwarded by the gateway as X-Authenticated-Name
     *   - authorities : comma-separated role string (e.g. ROLE_USER)
     *   - userId      : user's UUID — forwarded by the gateway as X-Authenticated-User-Id
     */
    public String generateToken(Authentication auth) {
        String authorities = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        // Resolve UUID from DB so downstream services can use it as FK
        UserAdmin user = userAdminRepo.findByEmail(auth.getName());
        String userId = user != null ? user.getId().toString() : "";
        String name = user != null ? user.getName() : "";

        return Jwts.builder()
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + JwtConstant.ACCESS_TOKEN_EXPIRY_MS))
                .claim("email", auth.getName())
                .claim("name", name)
                .claim("authorities", authorities)
                .claim("userId", userId)
                .signWith(key)
                .compact();
    }

    public String getEmailFromToken(String jwt) {
        jwt = jwt.substring(7);
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(jwt).getBody();
        return String.valueOf(claims.get("email"));
    }
}
