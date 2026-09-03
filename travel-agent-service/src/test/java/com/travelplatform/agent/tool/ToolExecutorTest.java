package com.travelplatform.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.travelplatform.agent.client.AuthHeaders;
import com.travelplatform.agent.client.BusClient;
import com.travelplatform.agent.client.DestinationClient;
import com.travelplatform.agent.client.HotelClient;
import com.travelplatform.agent.client.PackageClient;
import com.travelplatform.agent.client.RideClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Verifies ToolExecutor maps each tool name + JSON input to the correct
 * client call with the correct parameters — nothing here touches Anthropic
 * or a real downstream service (see the roadmap's test order: this is step
 * 1, before domain client integration tests and the /agent/chat smoke test).
 */
@ExtendWith(MockitoExtension.class)
class ToolExecutorTest {

    @Mock private DestinationClient destinationClient;
    @Mock private PackageClient packageClient;
    @Mock private HotelClient hotelClient;
    @Mock private BusClient busClient;
    @Mock private RideClient rideClient;

    private ToolExecutor toolExecutor;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ToolResultNormalizer normalizer = new ToolResultNormalizer(mapper);
    private final AuthHeaders auth = new AuthHeaders("user@example.com", "Test User", "ROLE_USER", "user-id-123");

    @BeforeEach
    void setUp() {
        toolExecutor = new ToolExecutor(destinationClient, packageClient, hotelClient, busClient, rideClient, normalizer);
    }

    private ObjectNode json() {
        return mapper.createObjectNode();
    }

    // ── search_destinations ─────────────────────────────────────────────

    @Test
    void searchDestinations_passesAllFieldsToClient() {
        ObjectNode input = json()
                .put("keyword", "beach")
                .put("category", "BEACH")
                .put("maxBudget", 15000.0)
                .put("visitMonth", 12);

        JsonNode expected = mapper.createObjectNode();
        when(destinationClient.search("beach", "BEACH", 15000.0, 12, auth)).thenReturn(expected);

        Object result = toolExecutor.execute("search_destinations", input, auth);

        verify(destinationClient).search("beach", "BEACH", 15000.0, 12, auth);
        assertThat(result).isEqualTo(expected);
        verifyNoInteractions(packageClient, hotelClient, busClient, rideClient);
    }

    @Test
    void searchDestinations_missingOptionalFieldsPassedAsNull() {
        ObjectNode input = json().put("keyword", "hills");

        toolExecutor.execute("search_destinations", input, auth);

        verify(destinationClient).search(eq("hills"), isNull(), isNull(), isNull(), eq(auth));
    }

    // ── search_packages ──────────────────────────────────────────────────

    @Test
    void searchPackages_passesAllFieldsToClient() {
        ObjectNode input = json()
                .put("destinationId", "11111111-1111-1111-1111-111111111111")
                .put("keyword", "honeymoon")
                .put("maxBudget", 20000.0)
                .put("minDurationDays", 3)
                .put("maxDurationDays", 5);

        toolExecutor.execute("search_packages", input, auth);

        verify(packageClient).search(
                "11111111-1111-1111-1111-111111111111", "honeymoon", 20000.0, 3, 5, auth);
    }

    // ── search_hotels ────────────────────────────────────────────────────

    @Test
    void searchHotels_passesAllFieldsToClient() {
        ObjectNode input = json()
                .put("destinationId", "22222222-2222-2222-2222-222222222222")
                .put("starRating", 4)
                .put("roomType", "DOUBLE")
                .put("minPrice", 2000.0)
                .put("maxPrice", 6000.0);

        toolExecutor.execute("search_hotels", input, auth);

        verify(hotelClient).search(
                "22222222-2222-2222-2222-222222222222", 4, "DOUBLE", 2000.0, 6000.0, auth);
    }

    @Test
    void searchHotels_unwrapsSpringPageContentToBareArray() {
        // Simulates the real GET /hotels shape: Page<HotelResponse>.
        ObjectNode page = json();
        page.putArray("content")
                .add(json().put("id", "h1").put("name", "Sea View Resort"));
        page.put("totalElements", 1);
        page.put("totalPages", 1);
        page.put("number", 0);

        when(hotelClient.search(any(), any(), any(), any(), any(), eq(auth))).thenReturn(page);

        Object result = toolExecutor.execute("search_hotels", json(), auth);

        assertThat(result).isInstanceOf(JsonNode.class);
        JsonNode arr = (JsonNode) result;
        assertThat(arr.isArray()).as("hotel result should be a bare array, not the Page wrapper").isTrue();
        assertThat(arr).hasSize(1);
        assertThat(arr.get(0).get("name").asText()).isEqualTo("Sea View Resort");
        // Page metadata should not leak through to the model.
        assertThat(arr.toString()).doesNotContain("totalElements");
    }

    // ── search_buses ─────────────────────────────────────────────────────

    @Test
    void searchBuses_passesRequiredFieldsAndParsesDate() {
        ObjectNode input = json()
                .put("source", "Hyderabad")
                .put("destination", "Bangalore")
                .put("date", "2026-09-15");

        toolExecutor.execute("search_buses", input, auth);

        verify(busClient).search("Hyderabad", "Bangalore", LocalDate.of(2026, 9, 15), auth);
        verifyNoInteractions(rideClient);
    }

    @Test
    void searchBuses_unwrapsBusResponseWrapperToBareArray() {
        // Simulates the real BusResponse shape: {message, success, bus, busses}.
        ObjectNode wrapper = json();
        wrapper.put("message", "OK");
        wrapper.put("success", true);
        wrapper.putNull("bus");
        wrapper.putArray("busses")
                .add(json().put("id", "b1").put("source", "Hyderabad").put("price", 800));

        when(busClient.search(any(), any(), any(), eq(auth))).thenReturn(wrapper);

        ObjectNode input = json().put("source", "Hyderabad").put("destination", "Bangalore").put("date", "2026-09-15");
        Object result = toolExecutor.execute("search_buses", input, auth);

        assertThat(result).isInstanceOf(JsonNode.class);
        JsonNode arr = (JsonNode) result;
        assertThat(arr.isArray()).as("bus result should be a bare array, not the BusResponse wrapper").isTrue();
        assertThat(arr).hasSize(1);
        assertThat(arr.get(0).get("price").asInt()).isEqualTo(800);
        // The wrapper's own fields should not leak through to the model.
        assertThat(arr.toString()).doesNotContain("\"success\"");
    }

    @Test
    void searchBuses_missingRequiredFieldReturnsFailureMessageInsteadOfThrowing() {
        ObjectNode input = json().put("source", "Hyderabad"); // destination and date missing

        Object result = toolExecutor.execute("search_buses", input, auth);

        assertThat(result).asString().contains("temporarily unavailable");
        verifyNoInteractions(busClient);
    }

    @Test
    void searchBuses_blankRequiredFieldReturnsFailureMessage() {
        ObjectNode input = json()
                .put("source", "  ")
                .put("destination", "Bangalore")
                .put("date", "2026-09-15");

        Object result = toolExecutor.execute("search_buses", input, auth);

        assertThat(result).asString().contains("temporarily unavailable");
        verifyNoInteractions(busClient);
    }

    // ── search_rides ─────────────────────────────────────────────────────

    @Test
    void searchRides_passesRequiredFieldsAndParsesDate() {
        ObjectNode input = json()
                .put("source", "Chennai")
                .put("destination", "Pondicherry")
                .put("date", "2026-10-01");

        toolExecutor.execute("search_rides", input, auth);

        verify(rideClient).search("Chennai", "Pondicherry", LocalDate.of(2026, 10, 1), auth);
    }

    // ── unknown tool / failure handling ─────────────────────────────────

    @Test
    void unknownToolName_returnsDescriptiveStringWithoutCallingAnyClient() {
        Object result = toolExecutor.execute("search_flights", json(), auth);

        assertThat(result).isEqualTo("Unknown tool: search_flights");
        verifyNoInteractions(destinationClient, packageClient, hotelClient, busClient, rideClient);
    }

    @Test
    void clientThrowingException_isCaughtAndReturnedAsFailureMessage() {
        when(destinationClient.search(any(), any(), any(), any(), eq(auth)))
                .thenThrow(new RuntimeException("service unavailable"));

        Object result = toolExecutor.execute("search_destinations", json(), auth);

        assertThat(result).asString().contains("temporarily unavailable");
        assertThat(result).asString().contains("service unavailable");
    }
}
