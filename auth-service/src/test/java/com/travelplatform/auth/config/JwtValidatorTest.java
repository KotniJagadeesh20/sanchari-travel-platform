package com.travelplatform.auth.config;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtValidatorTest {

    private JwtValidator jwtValidator;
    private SecretKey key;

    @BeforeEach
    void setUp() {
        jwtValidator = new JwtValidator();
        key = Keys.hmacShaKeyFor(JwtConstant.SECRET_KEY.getBytes());
        // Clear SecurityContext before each test
        SecurityContextHolder.clearContext();
    }

    // ─── Helper to build a signed JWT ────────────────────────────────────────

    private String buildToken(String email, String authorities, long expiryOffsetMs) {
        return Jwts.builder()
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiryOffsetMs))
                .claim("email", email)
                .claim("authorities", authorities)
                .signWith(key)
                .compact();
    }

    // ─── Tests ───────────────────────────────────────────────────────────────

    @Test
    void doFilterInternal_setsAuthentication_whenTokenIsValid() throws Exception {
        String token = buildToken("asha@example.com", "ROLE_USER", 60_000);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        jwtValidator.doFilterInternal(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth, "SecurityContext must be populated for a valid JWT");
        assertEquals("asha@example.com", auth.getName());
        assertTrue(auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @Test
    void doFilterInternal_setsMultipleAuthorities_whenTokenContainsThem() throws Exception {
        String token = buildToken("admin@example.com", "ROLE_ADMIN,ROLE_USER", 60_000);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);

        jwtValidator.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertEquals(2, auth.getAuthorities().size());
    }

    @Test
    void doFilterInternal_doesNotSetAuthentication_whenNoHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(); // no Authorization header
        jwtValidator.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNull(auth, "SecurityContext must stay empty when no Authorization header is present");
    }

    @Test
    void doFilterInternal_throwsBadCredentials_whenTokenIsExpired() {
        String expiredToken = buildToken("asha@example.com", "ROLE_USER", -10_000); // expired 10s ago

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + expiredToken);

        assertThrows(BadCredentialsException.class,
                () -> jwtValidator.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain()));
    }

    @Test
    void doFilterInternal_throwsBadCredentials_whenTokenIsTampered() {
        String validToken = buildToken("asha@example.com", "ROLE_USER", 60_000);
        // Tamper with the payload segment
        String[] parts = validToken.split("\\.");
        String tampered = parts[0] + "." + parts[1] + "TAMPERED." + parts[2];

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + tampered);

        assertThrows(BadCredentialsException.class,
                () -> jwtValidator.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain()));
    }

    @Test
    void doFilterInternal_throwsBadCredentials_whenTokenSignedWithWrongKey() {
        SecretKey wrongKey = Keys.hmacShaKeyFor(("wrongwrongwrongwrongwrongwrongwrongwrong").getBytes());
        String badToken = Jwts.builder()
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .claim("email", "hack@example.com")
                .claim("authorities", "ROLE_ADMIN")
                .signWith(wrongKey)
                .compact();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + badToken);

        assertThrows(BadCredentialsException.class,
                () -> jwtValidator.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain()));
    }

    @Test
    void doFilterInternal_setsEmptyAuthorities_whenAuthoritiesClaimMissing() throws Exception {
        // Token with no authorities claim
        String token = Jwts.builder()
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .claim("email", "noauth@example.com")
                // deliberately omit "authorities" claim
                .signWith(key)
                .compact();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);

        jwtValidator.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertTrue(auth.getAuthorities().isEmpty(),
                "Missing authorities claim should produce empty authority list, not NPE");
    }

    @Test
    void doFilterInternal_continuesFilterChain_afterSettingAuthentication() throws Exception {
        String token = buildToken("asha@example.com", "ROLE_USER", 60_000);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockFilterChain chain = new MockFilterChain();

        jwtValidator.doFilterInternal(request, new MockHttpServletResponse(), chain);

        // If the chain was invoked, getRequest() will be non-null
        assertNotNull(chain.getRequest(), "Filter chain must always continue after JWT processing");
    }
}
