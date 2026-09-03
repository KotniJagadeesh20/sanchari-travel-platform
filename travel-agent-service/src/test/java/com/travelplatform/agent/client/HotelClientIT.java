package com.travelplatform.agent.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies HotelClient against the real running stack. GET /hotels returns
 * Spring Data's Page<HotelResponse> — unlike the other four search
 * endpoints, which all return bare arrays — so this test asserts that shape
 * explicitly ({"content": [...], "totalElements": N, ...}) rather than
 * assuming an array. That's deliberate: per the decision to keep the Hotel
 * API's Page response as-is and instead decide, based on what this test
 * confirms, whether HotelClient itself should normalize the shape before
 * handing it to the tool loop (see the roadmap note on tool-result design).
 *
 * Run with: mvn -pl travel-agent-service verify (stack must be running)
 */
@SpringBootTest
class HotelClientIT {

    @Autowired
    private HotelClient hotelClient;

    private final AuthHeaders auth = new AuthHeaders("it-test@example.com", "IT Test", "ROLE_USER", "it-test-user");

    @Test
    void searchWithNoFilters_returnsSpringPageShape() {
        JsonNode result = hotelClient.search(null, null, null, null, null, auth);

        assertThat(result).isNotNull();
        assertThat(result.isArray())
                .as("Hotels should NOT be a bare array — confirming it's still Page<HotelResponse>")
                .isFalse();
        assertThat(result.has("content"))
                .as("Page response should have a 'content' array")
                .isTrue();
        assertThat(result.get("content").isArray()).isTrue();
        // Confirms it's a real Spring Data Page and not just any object with
        // a "content" field — these are Page-specific properties.
        assertThat(result.has("totalElements")).isTrue();
        assertThat(result.has("totalPages")).isTrue();
        assertThat(result.has("number")).as("current page index field").isTrue();
    }

    @Test
    void searchWithRealRoomTypeEnum_doesNotError() {
        // DELUXE is a real RoomType value (confirmed against the enum source)
        // — this is the value ToolRegistry now advertises to the model.
        JsonNode result = hotelClient.search(null, null, "DELUXE", null, null, auth);

        assertThat(result).isNotNull();
        assertThat(result.has("content")).isTrue();
    }

    @Test
    void content_matchesExpectedResponseFields() {
        JsonNode result = hotelClient.search(null, null, null, null, null, auth);
        JsonNode content = result.get("content");

        if (content.size() > 0) {
            JsonNode first = content.get(0);
            assertThat(first.has("id")).as("hotel id field").isTrue();
            assertThat(first.has("name")).as("hotel name field").isTrue();
            assertThat(first.has("starRating")).as("hotel starRating field").isTrue();
        }
    }
}
