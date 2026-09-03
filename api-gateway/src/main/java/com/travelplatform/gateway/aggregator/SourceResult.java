package com.travelplatform.gateway.aggregator;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Outcome of fetching one booking source. `warning` is null on success
 * (including a legitimate "this user has none" empty result) and non-null
 * only when the source's own data couldn't be fetched (timeout, 5xx, circuit
 * open) — see BookingAggregatorService for why those two "empty" cases are
 * kept distinct instead of collapsing to the same thing.
 */
public record SourceResult(String key, JsonNode data, String warning) {}
