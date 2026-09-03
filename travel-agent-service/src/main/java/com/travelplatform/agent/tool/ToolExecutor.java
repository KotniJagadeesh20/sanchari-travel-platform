package com.travelplatform.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.travelplatform.agent.client.AuthHeaders;
import com.travelplatform.agent.client.BusClient;
import com.travelplatform.agent.client.DestinationClient;
import com.travelplatform.agent.client.HotelClient;
import com.travelplatform.agent.client.PackageClient;
import com.travelplatform.agent.client.RideClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Set;

/**
 * Executes one tool_use block (name + input JSON, as returned by Anthropic)
 * against the matching client wrapper, and returns the raw JsonNode result
 * to be serialized back as a tool_result content block. A tool failing
 * (downstream service down, bad city name, etc) becomes a text description
 * of the failure fed back to the model as the tool_result — the model
 * decides how to communicate that to the user, rather than this code
 * silently swallowing it or throwing and killing the whole turn.
 */
@Component
public class ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(ToolExecutor.class);
    private static final Set<String> KNOWN_TOOLS = Set.of(
            "search_destinations", "search_packages", "search_hotels", "search_buses", "search_rides");

    private final DestinationClient destinationClient;
    private final PackageClient packageClient;
    private final HotelClient hotelClient;
    private final BusClient busClient;
    private final RideClient rideClient;
    private final ToolResultNormalizer normalizer;

    public ToolExecutor(DestinationClient destinationClient, PackageClient packageClient,
                         HotelClient hotelClient, BusClient busClient, RideClient rideClient,
                         ToolResultNormalizer normalizer) {
        this.destinationClient = destinationClient;
        this.packageClient = packageClient;
        this.hotelClient = hotelClient;
        this.busClient = busClient;
        this.rideClient = rideClient;
        this.normalizer = normalizer;
    }

    public Object execute(String toolName, JsonNode input, AuthHeaders auth) {
        if (!KNOWN_TOOLS.contains(toolName)) {
            return "Unknown tool: " + toolName;
        }

        try {
            JsonNode raw = switch (toolName) {
                case "search_destinations" -> destinationClient.search(
                        text(input, "keyword"), text(input, "category"),
                        number(input, "maxBudget"), integer(input, "visitMonth"), auth);
                case "search_packages" -> packageClient.search(
                        text(input, "destinationId"), text(input, "keyword"),
                        number(input, "maxBudget"), integer(input, "minDurationDays"), integer(input, "maxDurationDays"), auth);
                case "search_hotels" -> hotelClient.search(
                        text(input, "destinationId"), integer(input, "starRating"), text(input, "roomType"),
                        number(input, "minPrice"), number(input, "maxPrice"), auth);
                case "search_buses" -> busClient.search(
                        requireText(input, "source"), requireText(input, "destination"),
                        LocalDate.parse(requireText(input, "date")), auth);
                default -> rideClient.search(
                        requireText(input, "source"), requireText(input, "destination"),
                        LocalDate.parse(requireText(input, "date")), auth);
            };

            // Every known tool's result is unwrapped to a plain array here —
            // see ToolResultNormalizer for why Hotels/Buses need it and
            // Destinations/Packages/Rides pass through unchanged.
            return normalizer.normalize(toolName, raw);
        } catch (Exception e) {
            log.warn("Tool execution failed for {}: {}", toolName, e.toString());
            return "This search is temporarily unavailable (" + e.getMessage() + "). " +
                    "Let the user know and suggest trying again shortly, or a different query.";
        }
    }

    private String text(JsonNode input, String field) {
        JsonNode v = input.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }

    private String requireText(JsonNode input, String field) {
        String v = text(input, field);
        if (v == null || v.isBlank()) throw new IllegalArgumentException("Missing required field: " + field);
        return v;
    }

    private Double number(JsonNode input, String field) {
        JsonNode v = input.get(field);
        return (v == null || v.isNull()) ? null : v.asDouble();
    }

    private Integer integer(JsonNode input, String field) {
        JsonNode v = input.get(field);
        return (v == null || v.isNull()) ? null : v.asInt();
    }
}
