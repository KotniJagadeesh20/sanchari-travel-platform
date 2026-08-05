package com.travelplatform.rideshare.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request body for booking seats on a ride")
public class BookRideRequest {

    @NotNull(message = "Seats is required")
    @Min(value = 1, message = "Must book at least 1 seat")
    @Schema(description = "Number of seats to book", example = "2")
    private Integer seats;

    public Integer getSeats() { return seats; }
    public void setSeats(Integer seats) { this.seats = seats; }
}
