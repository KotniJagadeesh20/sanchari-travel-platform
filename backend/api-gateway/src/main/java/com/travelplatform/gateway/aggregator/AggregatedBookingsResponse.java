package com.travelplatform.gateway.aggregator;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Each field holds the RAW booking array as returned by that service's own
 * DTO — deliberately not normalized into one generic booking shape (per the
 * aggregator's design brief: aggregate, don't normalize). `warnings` is
 * present only when at least one source failed/timed out/tripped its
 * circuit breaker; it's omitted entirely on a fully-healthy response so the
 * happy-path shape matches the brief's example exactly.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AggregatedBookingsResponse {

    private JsonNode busBookings;
    private JsonNode rideBookings;
    private JsonNode hotelBookings;
    private JsonNode packageBookings;
    private List<String> warnings;

    public JsonNode getBusBookings() { return busBookings; }
    public void setBusBookings(JsonNode busBookings) { this.busBookings = busBookings; }

    public JsonNode getRideBookings() { return rideBookings; }
    public void setRideBookings(JsonNode rideBookings) { this.rideBookings = rideBookings; }

    public JsonNode getHotelBookings() { return hotelBookings; }
    public void setHotelBookings(JsonNode hotelBookings) { this.hotelBookings = hotelBookings; }

    public JsonNode getPackageBookings() { return packageBookings; }
    public void setPackageBookings(JsonNode packageBookings) { this.packageBookings = packageBookings; }

    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
}
