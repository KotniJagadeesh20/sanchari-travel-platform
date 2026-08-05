package com.travelplatform.rideshare.dto;

import com.travelplatform.rideshare.entity.Ride;
import com.travelplatform.rideshare.enums.RideStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Schema(description = "Ride details returned to clients — never exposes the raw entity or driver's internal data")
public class RideResponse {

    private UUID id;
    private String source;
    private String destination;
    private LocalDate travelDate;
    private LocalTime departureTime;
    private Integer totalSeats;
    private Integer availableSeats;
    private Double pricePerSeat;
    private String vehicleType;
    private String vehicleNumber;
    private String pickupPoint;
    private String dropPoint;
    private RideStatus status;
    private UUID driverId;
    private String driverName;
    private String driverEmail;

    public static RideResponse from(Ride ride) {
        RideResponse r = new RideResponse();
        r.id = ride.getId();
        r.source = ride.getSource();
        r.destination = ride.getDestination();
        r.travelDate = ride.getTravelDate();
        r.departureTime = ride.getDepartureTime();
        r.totalSeats = ride.getTotalSeats();
        r.availableSeats = ride.getAvailableSeats();
        r.pricePerSeat = ride.getPricePerSeat();
        r.vehicleType = ride.getVehicleType();
        r.vehicleNumber = ride.getVehicleNumber();
        r.pickupPoint = ride.getPickupPoint();
        r.dropPoint = ride.getDropPoint();
        r.status = ride.getStatus();
        r.driverId = ride.getDriver().getId();
        r.driverName = ride.getDriver().getName();
        r.driverEmail = ride.getDriver().getEmail();
        return r;
    }

    public UUID getId() { return id; }
    public String getSource() { return source; }
    public String getDestination() { return destination; }
    public LocalDate getTravelDate() { return travelDate; }
    public LocalTime getDepartureTime() { return departureTime; }
    public Integer getTotalSeats() { return totalSeats; }
    public Integer getAvailableSeats() { return availableSeats; }
    public Double getPricePerSeat() { return pricePerSeat; }
    public String getVehicleType() { return vehicleType; }
    public String getVehicleNumber() { return vehicleNumber; }
    public String getPickupPoint() { return pickupPoint; }
    public String getDropPoint() { return dropPoint; }
    public RideStatus getStatus() { return status; }
    public UUID getDriverId() { return driverId; }
    public String getDriverName() { return driverName; }
    public String getDriverEmail() { return driverEmail; }
}
