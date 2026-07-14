package com.travelplatform.rideshare.dto;

import com.travelplatform.rideshare.entity.RideBooking;
import com.travelplatform.rideshare.enums.BookingStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Booking details returned to clients")
public class RideBookingResponse {

    private UUID id;
    private UUID rideId;
    private String rideSource;
    private String rideDestination;
    private UUID passengerId;
    private String passengerName;
    private String passengerEmail;
    private Integer seatsBooked;
    private Double totalAmount;
    private BookingStatus status;
    private LocalDateTime bookingTime;

    public static RideBookingResponse from(RideBooking booking) {
        RideBookingResponse r = new RideBookingResponse();
        r.id = booking.getId();
        r.rideId = booking.getRide().getId();
        r.rideSource = booking.getRide().getSource();
        r.rideDestination = booking.getRide().getDestination();
        r.passengerId = booking.getPassenger().getId();
        r.passengerName = booking.getPassenger().getName();
        r.passengerEmail = booking.getPassenger().getEmail();
        r.seatsBooked = booking.getSeatsBooked();
        r.totalAmount = booking.getTotalAmount();
        r.status = booking.getStatus();
        r.bookingTime = booking.getBookingTime();
        return r;
    }

    public UUID getId() { return id; }
    public UUID getRideId() { return rideId; }
    public String getRideSource() { return rideSource; }
    public String getRideDestination() { return rideDestination; }
    public UUID getPassengerId() { return passengerId; }
    public String getPassengerName() { return passengerName; }
    public String getPassengerEmail() { return passengerEmail; }
    public Integer getSeatsBooked() { return seatsBooked; }
    public Double getTotalAmount() { return totalAmount; }
    public BookingStatus getStatus() { return status; }
    public LocalDateTime getBookingTime() { return bookingTime; }
}
