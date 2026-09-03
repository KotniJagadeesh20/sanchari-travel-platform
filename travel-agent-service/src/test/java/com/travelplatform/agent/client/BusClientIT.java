package com.travelplatform.agent.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies BusClient against the real running stack.
 *
 * IMPORTANT FINDING from writing this test: GET /api/user/searchbusses/...
 * does NOT return a bare array like search_destinations/search_packages/
 * search_rides do. It returns a BusResponse wrapper:
 *   { "message": ..., "success": true, "bus": null, "busses": [...] }
 * BusClient currently returns this whole wrapper as-is (matching what
 * ToolExecutor hands to the model). This test asserts that ACTUAL shape —
 * it does not assume an array — specifically so it fails loudly if the
 * wrapper shape ever changes, rather than silently passing against a wrong
 * assumption. Whether the agent should see the raw wrapper or a normalized
 * result (mirroring the Hotels Page<T> question) is a decision to make once
 * this is confirmed, not something to silently "fix" here.
 *
 * Run with: mvn -pl travel-agent-service verify (stack must be running)
 */
@SpringBootTest
class BusClientIT {

    @Autowired
    private BusClient busClient;

    private final AuthHeaders auth = new AuthHeaders("it-test@example.com", "IT Test", "ROLE_USER", "it-test-user");

    @Test
    void search_returnsBusResponseWrapperNotBareArray() {
        JsonNode result = busClient.search("Hyderabad", "Bangalore", LocalDate.now().plusDays(7), auth);

        assertThat(result).isNotNull();
        assertThat(result.isArray())
                .as("Bus search should NOT be a bare array — it's the BusResponse wrapper")
                .isFalse();
        assertThat(result.has("success")).as("wrapper 'success' field").isTrue();
        assertThat(result.has("busses")).as("wrapper 'busses' field (the actual list)").isTrue();
        assertThat(result.get("busses").isArray()).isTrue();
    }

    @Test
    void search_caseAndWhitespaceInsensitiveMatching() {
        LocalDate date = LocalDate.now().plusDays(7);
        JsonNode exact = busClient.search("Hyderabad", "Bangalore", date, auth);
        JsonNode messy = busClient.search(" HYDERABAD ", " bangalore", date, auth);

        assertThat(exact.get("busses").size())
                .as("Case/whitespace-insensitive query should return the same result count as the exact-case query")
                .isEqualTo(messy.get("busses").size());
    }

    @Test
    void bussesContent_matchesExpectedResponseFields() {
        JsonNode result = busClient.search("Hyderabad", "Bangalore", LocalDate.now().plusDays(7), auth);
        JsonNode busses = result.get("busses");

        if (busses.size() > 0) {
            JsonNode first = busses.get(0);
            assertThat(first.has("id")).as("bus id field").isTrue();
            assertThat(first.has("source")).as("bus source field").isTrue();
            assertThat(first.has("price")).as("bus price field").isTrue();
        }
    }

    @Test
    void requiresAuthentication_matchesToolDescription() {
        // search_buses is documented (in ToolRegistry) as requiring
        // authentication, unlike the other four tools. This is here mainly
        // as living documentation of that asymmetry — if bus-booking-service's
        // SecurityConfig ever changes /api/** to permitAll, this test still
        // passes (auth headers are always sent), but the asymmetry note in
        // ToolRegistry would then be stale and worth revisiting.
        JsonNode result = busClient.search("Hyderabad", "Bangalore", LocalDate.now().plusDays(7), auth);
        assertThat(result).isNotNull();
    }
}
