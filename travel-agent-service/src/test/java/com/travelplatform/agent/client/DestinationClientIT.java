package com.travelplatform.agent.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies DestinationClient against the REAL running stack (Eureka +
 * travel-packages-service) — not a mock. Requires `docker-compose up`
 * (or the equivalent local run) BEFORE this test runs.
 *
 * Run with: mvn -pl travel-agent-service verify
 * (excluded from `mvn test` — see the failsafe-plugin note in pom.xml)
 *
 * What this checks that a mocked test cannot:
 *   - the endpoint path (/destinations/search) and query params actually
 *     exist and are spelled the way DestinationClient assumes
 *   - lb://travel-packages-service resolves via Eureka to a live instance
 *   - the JSON response really is a bare array of objects (not wrapped,
 *     not paginated) — see the note about Hotels being different
 *   - the service responds at all (catches "service is down" as a hard
 *     test failure, not a silent null)
 */
@SpringBootTest
class DestinationClientIT {

    @Autowired
    private DestinationClient destinationClient;

    private final AuthHeaders auth = new AuthHeaders("it-test@example.com", "IT Test", "ROLE_USER", "it-test-user");

    @Test
    void searchWithNoFilters_returnsArray() {
        JsonNode result = destinationClient.search(null, null, null, null, auth);

        assertThat(result).isNotNull();
        assertThat(result.isArray())
                .as("Expected a bare JSON array from /destinations/search, got: %s", result.getNodeType())
                .isTrue();
    }

    @Test
    void searchWithCategoryFilter_usesRealEnumValue() {
        // BEACH is a real DestinationCategory value (confirmed against the
        // entity source, not guessed) — a 400 here means either the enum
        // changed or the param name/casing DestinationClient sends is wrong.
        JsonNode result = destinationClient.search(null, "BEACH", null, null, auth);

        assertThat(result).isNotNull();
        assertThat(result.isArray()).isTrue();
    }

    @Test
    void searchWithKeyword_matchesExpectedResponseFields() {
        JsonNode result = destinationClient.search("a", null, null, null, auth);

        assertThat(result.isArray()).isTrue();
        if (result.size() > 0) {
            JsonNode first = result.get(0);
            // Field names DestinationClient/ToolRegistry's description implicitly
            // promises the model — if these don't exist, the agent's answers
            // about destinations will be wrong even though the call "succeeds".
            assertThat(first.has("id")).as("destination id field").isTrue();
            assertThat(first.has("name")).as("destination name field").isTrue();
        }
    }
}
