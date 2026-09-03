package com.travelplatform.agent.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies RideClient against the real running stack.
 * Run with: mvn -pl travel-agent-service verify (stack must be running)
 */
@SpringBootTest
class RideClientIT {

    @Autowired
    private RideClient rideClient;

    private final AuthHeaders auth = new AuthHeaders("it-test@example.com", "IT Test", "ROLE_USER", "it-test-user");

    @Test
    void search_returnsBareArray() {
        JsonNode result = rideClient.search("Chennai", "Pondicherry", LocalDate.now().plusDays(7), auth);

        assertThat(result).isNotNull();
        assertThat(result.isArray())
                .as("Expected a bare JSON array from /rides/search, got: %s", result.getNodeType())
                .isTrue();
    }

    @Test
    void search_caseAndWhitespaceInsensitiveMatching() {
        LocalDate date = LocalDate.now().plusDays(7);
        JsonNode exact = rideClient.search("Chennai", "Pondicherry", date, auth);
        JsonNode messy = rideClient.search(" CHENNAI ", " pondicherry", date, auth);

        assertThat(exact.size())
                .as("Case/whitespace-insensitive query should return the same result count as the exact-case query")
                .isEqualTo(messy.size());
    }

    @Test
    void results_matchExpectedResponseFields() {
        JsonNode result = rideClient.search("Chennai", "Pondicherry", LocalDate.now().plusDays(7), auth);

        if (result.size() > 0) {
            JsonNode first = result.get(0);
            assertThat(first.has("id")).as("ride id field").isTrue();
            assertThat(first.has("source")).as("ride source field").isTrue();
            assertThat(first.has("pricePerSeat")).as("ride pricePerSeat field").isTrue();
        }
    }

    @Test
    void search_worksWithoutAuthentication() {
        // Unlike Bus, /rides/search is public — pass empty auth and confirm
        // it still succeeds, matching what ToolRegistry documents.
        AuthHeaders noAuth = new AuthHeaders(null, null, null, null);
        JsonNode result = rideClient.search("Chennai", "Pondicherry", LocalDate.now().plusDays(7), noAuth);
        assertThat(result).isNotNull();
        assertThat(result.isArray()).isTrue();
    }
}
