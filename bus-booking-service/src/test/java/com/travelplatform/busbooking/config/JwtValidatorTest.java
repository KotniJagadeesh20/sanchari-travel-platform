package com.travelplatform.busbooking.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Verifies that bus-booking-service trusts the X-Authenticated-* headers
 * forwarded by the API Gateway, rather than parsing a JWT itself.
 */
class JwtValidatorTest {

    private JwtValidator jwtValidator;

    @BeforeEach
    void setUp() {
        jwtValidator = new JwtValidator();
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_setsAuthentication_whenEmailHeaderPresent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Authenticated-Email", "asha@example.com");
        request.addHeader("X-Authenticated-Authorities", "ROLE_USER");

        jwtValidator.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth, "SecurityContext must be populated when gateway forwards identity headers");
        assertEquals("asha@example.com", auth.getName());
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @Test
    void doFilterInternal_setsMultipleAuthorities_whenCommaSeparated() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Authenticated-Email", "admin@example.com");
        request.addHeader("X-Authenticated-Authorities", "ROLE_ADMIN,ROLE_USER");

        jwtValidator.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertEquals(2, auth.getAuthorities().size());
    }

    @Test
    void doFilterInternal_doesNotSetAuthentication_whenNoEmailHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(); // no headers at all

        jwtValidator.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication(),
                "SecurityContext must stay empty when the gateway hasn't forwarded identity (e.g. public paths)");
    }

    @Test
    void doFilterInternal_setsEmptyAuthorities_whenAuthoritiesHeaderMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Authenticated-Email", "noauth@example.com");
        // deliberately no X-Authenticated-Authorities header

        jwtValidator.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertTrue(auth.getAuthorities().isEmpty(),
                "Missing authorities header should produce empty list, not a failure");
    }

    @Test
    void doFilterInternal_ignoresBlankEmailHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Authenticated-Email", "");

        jwtValidator.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_continuesFilterChain_regardlessOfHeaders() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Authenticated-Email", "asha@example.com");
        MockFilterChain chain = new MockFilterChain();

        jwtValidator.doFilterInternal(request, new MockHttpServletResponse(), chain);

        assertNotNull(chain.getRequest(), "Filter chain must always continue");
    }
}
