package com.travelplatform.rideshare.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "Request body for creating a new ride offer")
public class CreateRideRequest {

    @NotBlank(message = "Source is required")
    @Schema(description = "Departure city", example = "Hyderabad")
    private String source;

    @NotBlank(message = "Destination is required")
    @Schema(description = "Arrival city", example = "Vijayawada")
    private String destination;

    @NotNull(message = "Travel date is required")
    @FutureOrPresent(message = "Travel date cannot be in the past")
    @Schema(description = "Date of travel", example = "2026-06-25")
    private LocalDate travelDate;

    @NotNull(message = "Departure time is required")
    @Schema(description = "Departure time", example = "08:00")
    private LocalTime departureTime;

    @NotNull(message = "Total seats is required")
    @Min(value = 1, message = "Must offer at least 1 seat")
    @Max(value = 8, message = "Cannot exceed 8 seats per ride")
    @Schema(description = "Total seats offered", example = "4")
    private Integer totalSeats;

    @NotNull(message = "Price per seat is required")
    @Positive(message = "Price per seat must be positive")
    @Schema(description = "Price per seat in local currency", example = "500.0")
    private Double pricePerSeat;

    @Schema(description = "Vehicle type (optional)", example = "Sedan")
    private String vehicleType;

    @Schema(description = "Vehicle number (optional)", example = "AP09AB1234")
    private String vehicleNumber;

    @Schema(description = "Specific pickup spot within the source city (optional)", example = "Ameerpet Metro Station")
    private String pickupPoint;

    @Schema(description = "Specific drop-off spot within the destination city (optional)", example = "Dwaraka Bus Stand")
    private String dropPoint;

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public LocalDate getTravelDate() { return travelDate; }
    public void setTravelDate(LocalDate travelDate) { this.travelDate = travelDate; }

    public LocalTime getDepartureTime() { return departureTime; }
    public void setDepartureTime(LocalTime departureTime) { this.departureTime = departureTime; }

    public Integer getTotalSeats() { return totalSeats; }
    public void setTotalSeats(Integer totalSeats) { this.totalSeats = totalSeats; }

    public Double getPricePerSeat() { return pricePerSeat; }
    public void setPricePerSeat(Double pricePerSeat) { this.pricePerSeat = pricePerSeat; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }

    public String getPickupPoint() { return pickupPoint; }
    public void setPickupPoint(String pickupPoint) { this.pickupPoint = pickupPoint; }

    public String getDropPoint() { return dropPoint; }
    public void setDropPoint(String dropPoint) { this.dropPoint = dropPoint; }
}
