package com.travelplatform.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ToolResultNormalizerTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ToolResultNormalizer normalizer = new ToolResultNormalizer(mapper);

    private ObjectNode obj() {
        return mapper.createObjectNode();
    }

    @Test
    void passThroughTools_returnBareArrayUnchanged() {
        JsonNode array = mapper.createArrayNode().add(obj().put("id", "d1"));

        for (String tool : new String[]{"search_destinations", "search_packages", "search_rides"}) {
            JsonNode result = normalizer.normalize(tool, array);
            assertThat(result).isSameAs(array);
        }
    }

    @Test
    void searchHotels_unwrapsContentField() {
        ObjectNode page = obj();
        page.putArray("content").add(obj().put("id", "h1"));
        page.put("totalElements", 1);

        JsonNode result = normalizer.normalize("search_hotels", page);

        assertThat(result.isArray()).isTrue();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("id").asText()).isEqualTo("h1");
    }

    @Test
    void searchBuses_unwrapsBussesField() {
        ObjectNode wrapper = obj();
        wrapper.put("success", true);
        wrapper.putArray("busses").add(obj().put("id", "b1"));

        JsonNode result = normalizer.normalize("search_buses", wrapper);

        assertThat(result.isArray()).isTrue();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("id").asText()).isEqualTo("b1");
    }

    @Test
    void searchHotels_missingContentField_returnsEmptyArrayNotError() {
        ObjectNode malformed = obj().put("unexpected", "shape");

        JsonNode result = normalizer.normalize("search_hotels", malformed);

        assertThat(result.isArray()).isTrue();
        assertThat(result).isEmpty();
    }

    @Test
    void searchBuses_missingBussesField_returnsEmptyArrayNotError() {
        ObjectNode malformed = obj().put("unexpected", "shape");

        JsonNode result = normalizer.normalize("search_buses", malformed);

        assertThat(result.isArray()).isTrue();
        assertThat(result).isEmpty();
    }

    @Test
    void alreadyArrayInput_passesThroughEvenForHotelsOrBuses() {
        // If an underlying endpoint's shape ever changes to a bare array,
        // the normalizer shouldn't try to index into it as an object.
        JsonNode array = mapper.createArrayNode().add(obj().put("id", "x"));

        assertThat(normalizer.normalize("search_hotels", array)).isSameAs(array);
        assertThat(normalizer.normalize("search_buses", array)).isSameAs(array);
    }

    @Test
    void nullOrMissingRaw_returnsEmptyArray() {
        assertThat(normalizer.normalize("search_hotels", null).isArray()).isTrue();
        assertThat(normalizer.normalize("search_hotels", null)).isEmpty();

        assertThat(normalizer.normalize("search_buses", mapper.nullNode()).isArray()).isTrue();
    }

    @Test
    void unknownTool_returnsRawUnchanged() {
        JsonNode raw = obj().put("whatever", "shape");
        assertThat(normalizer.normalize("some_future_tool", raw)).isSameAs(raw);
    }
}
