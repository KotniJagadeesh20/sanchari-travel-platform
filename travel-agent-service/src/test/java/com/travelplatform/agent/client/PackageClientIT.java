package com.travelplatform.agent.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies PackageClient against the real running stack. Particularly
 * important here since GET /packages/search is a new endpoint (added as
 * part of the search API cleanup) — this is the first time it's been
 * exercised outside of code review.
 *
 * Run with: mvn -pl travel-agent-service verify (stack must be running)
 */
@SpringBootTest
class PackageClientIT {

    @Autowired
    private PackageClient packageClient;

    private final AuthHeaders auth = new AuthHeaders("it-test@example.com", "IT Test", "ROLE_USER", "it-test-user");

    @Test
    void searchWithNoFilters_returnsArray() {
        JsonNode result = packageClient.search(null, null, null, null, null, auth);

        assertThat(result).isNotNull();
        assertThat(result.isArray())
                .as("Expected a bare JSON array from /packages/search, got: %s", result.getNodeType())
                .isTrue();
    }

    @Test
    void searchWithMaxBudget_filtersRatherThanErroring() {
        JsonNode result = packageClient.search(null, null, 1.0, null, null, auth);

        assertThat(result).isNotNull();
        assertThat(result.isArray()).isTrue();
        // maxBudget=1.0 should exclude essentially everything (a 400/500 here
        // would mean the new endpoint's price filter is broken, not just that
        // there happen to be no ₹1 packages).
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    void searchWithKeyword_matchesExpectedResponseFields() {
        JsonNode result = packageClient.search(null, "a", null, null, null, auth);

        assertThat(result.isArray()).isTrue();
        if (result.size() > 0) {
            JsonNode first = result.get(0);
            assertThat(first.has("id")).as("package id field").isTrue();
            assertThat(first.has("title")).as("package title field").isTrue();
            assertThat(first.has("price")).as("package price field").isTrue();
        }
    }

    @Test
    void durationFilters_areAppliedTogether() {
        // If minDurationDays/maxDurationDays aren't wired through correctly,
        // this would either error or silently ignore the filter — either way
        // worth catching here rather than discovering it via a confused agent.
        JsonNode result = packageClient.search(null, null, null, 100, 200, auth);

        assertThat(result).isNotNull();
        assertThat(result.isArray()).isTrue();
        assertThat(result.size())
                .as("No real package should have a 100-200 day duration")
                .isEqualTo(0);
    }
}
