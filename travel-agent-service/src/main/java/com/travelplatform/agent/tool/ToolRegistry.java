package com.travelplatform.agent.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

/**
 * Phase A tool schemas — search_destinations/packages/hotels/buses/rides,
 * one per existing search API. Each input_schema mirrors that endpoint's
 * actual query params (see the client wrappers in .client and the search
 * API cleanup described in the roadmap) so the model can only ask for
 * things the underlying API actually supports.
 */
@Component
public class ToolRegistry {

    private final ObjectMapper mapper;

    public ToolRegistry(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public ArrayNode buildToolDefinitions() {
        ArrayNode tools = mapper.createArrayNode();
        tools.add(searchDestinationsTool());
        tools.add(searchPackagesTool());
        tools.add(searchHotelsTool());
        tools.add(searchBusesTool());
        tools.add(searchRidesTool());
        return tools;
    }

    private ObjectNode tool(String name, String description) {
        ObjectNode t = mapper.createObjectNode();
        t.put("name", name);
        t.put("description", description);
        return t;
    }

    private ObjectNode prop(String type, String description) {
        ObjectNode p = mapper.createObjectNode();
        p.put("type", type);
        if (description != null) p.put("description", description);
        return p;
    }

    private ObjectNode schema(ObjectNode properties, String... required) {
        ObjectNode s = mapper.createObjectNode();
        s.put("type", "object");
        s.set("properties", properties);
        if (required.length > 0) {
            ArrayNode req = mapper.createArrayNode();
            for (String r : required) req.add(r);
            s.set("required", req);
        }
        return s;
    }

    private ObjectNode searchDestinationsTool() {
        ObjectNode props = mapper.createObjectNode();
        props.set("keyword", prop("string", "Partial, case-insensitive destination name match"));
        props.set("category", prop("string", "Destination category — exactly one of: BEACH, HILL_STATION, ADVENTURE, RELIGIOUS, FAMILY, NATURE, ROAD_TRIP"));
        props.set("maxBudget", prop("number", "Maximum average budget for the destination"));
        props.set("visitMonth", prop("integer", "1=January..12=December — matches destinations whose best months include this month"));

        ObjectNode t = tool("search_destinations", "Search travel destinations by keyword, category, budget, and/or best visiting month. All filters are optional and combined with AND.");
        t.set("input_schema", schema(props));
        return t;
    }

    private ObjectNode searchPackagesTool() {
        ObjectNode props = mapper.createObjectNode();
        props.set("destinationId", prop("string", "UUID of a destination, from search_destinations results"));
        props.set("keyword", prop("string", "Partial, case-insensitive match against the package title or description"));
        props.set("maxBudget", prop("number", "Maximum package price"));
        props.set("minDurationDays", prop("integer", "Minimum trip length in days"));
        props.set("maxDurationDays", prop("integer", "Maximum trip length in days"));

        ObjectNode t = tool("search_packages", "Search bookable travel packages by destination, keyword, budget, and/or trip duration. All filters are optional and combined with AND.");
        t.set("input_schema", schema(props));
        return t;
    }

    private ObjectNode searchHotelsTool() {
        ObjectNode props = mapper.createObjectNode();
        props.set("destinationId", prop("string", "UUID of a destination, from search_destinations results"));
        props.set("starRating", prop("integer", "Exact star rating, 1-5"));
        props.set("roomType", prop("string", "Room type — exactly one of: STANDARD, DELUXE, SUITE, FAMILY"));
        props.set("minPrice", prop("number", "Minimum nightly room price"));
        props.set("maxPrice", prop("number", "Maximum nightly room price"));

        ObjectNode t = tool("search_hotels", "Search hotels, optionally filtered by destination, star rating, room type, and/or price range. All filters are optional and combined with AND.");
        t.set("input_schema", schema(props));
        return t;
    }

    private ObjectNode searchBusesTool() {
        ObjectNode props = mapper.createObjectNode();
        props.set("source", prop("string", "Departure city"));
        props.set("destination", prop("string", "Arrival city"));
        props.set("date", prop("string", "Travel date, format YYYY-MM-DD"));

        ObjectNode t = tool("search_buses", "Search scheduled buses between two cities on a given date. All three parameters are required. City matching is case/whitespace-insensitive.");
        t.set("input_schema", schema(props, "source", "destination", "date"));
        return t;
    }

    private ObjectNode searchRidesTool() {
        ObjectNode props = mapper.createObjectNode();
        props.set("source", prop("string", "Departure city"));
        props.set("destination", prop("string", "Arrival city"));
        props.set("date", prop("string", "Travel date, format YYYY-MM-DD"));

        ObjectNode t = tool("search_rides", "Search scheduled shared rides between two cities on a given date. All three parameters are required. City matching is case/whitespace-insensitive.");
        t.set("input_schema", schema(props, "source", "destination", "date"));
        return t;
    }
}
