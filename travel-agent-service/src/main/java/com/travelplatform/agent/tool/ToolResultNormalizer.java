package com.travelplatform.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Unwraps each domain service's actual response shape into a plain array,
 * so the model always sees search_X -> [...] regardless of what the
 * underlying service returns. Confirmed via the client integration tests:
 *
 *   search_destinations -> bare array already                (pass through)
 *   search_packages     -> bare array already                (pass through)
 *   search_rides         -> bare array already                (pass through)
 *   search_hotels        -> Page<HotelResponse>: unwrap "content"
 *   search_buses         -> {message, success, bus, busses}: unwrap "busses"
 *
 * Deliberately does NOT touch the domain services — this only reshapes the
 * response after it's already been received, entirely inside
 * travel-agent-service. Item-level fields (price, dates, availability, etc.)
 * are left exactly as the domain service returned them; only the outer
 * container shape changes.
 */
@Component
public class ToolResultNormalizer {

    private static final Logger log = LoggerFactory.getLogger(ToolResultNormalizer.class);

    private final ObjectMapper mapper;

    public ToolResultNormalizer(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public JsonNode normalize(String toolName, JsonNode raw) {
        if (raw == null || raw.isNull()) {
            return mapper.createArrayNode();
        }

        return switch (toolName) {
            case "search_hotels" -> unwrap(raw, "content");
            case "search_buses" -> unwrap(raw, "busses");
            // search_destinations, search_packages, search_rides — already bare
            // arrays; anything else falls through unchanged too, rather than
            // silently dropping data for a tool this normalizer doesn't know
            // about yet.
            default -> raw;
        };
    }

    private JsonNode unwrap(JsonNode raw, String field) {
        if (raw.isArray()) {
            // Already an array (e.g. the underlying endpoint changed shape) —
            // nothing to unwrap.
            return raw;
        }
        JsonNode inner = raw.get(field);
        if (inner != null && inner.isArray()) {
            return inner;
        }
        // Field missing entirely (not just empty) — the contract changed
        // under us. Log loudly rather than silently returning an empty
        // result that looks like a legitimate "no matches" to the model.
        if (inner == null) {
            log.warn("Expected field '{}' not found while normalizing response: {}", field, raw);
        }
        return mapper.createArrayNode();
    }
}
