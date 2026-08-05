package com.travelplatform.hotel.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.UUID;

public class CreateBookingRequest {

    @NotNull(message = "hotelId is required")
    private UUID hotelId;

    @NotNull(message = "roomId is required")
    private UUID roomId;

    @NotNull(message = "checkInDate is required")
    @FutureOrPresent(message = "checkInDate cannot be in the past")
    private LocalDate checkInDate;

    @NotNull(message = "checkOutDate is required")
    @Future(message = "checkOutDate must be in the future")
    private LocalDate checkOutDate;

    @NotNull(message = "numberOfGuests is required")
    @Min(value = 1, message = "numberOfGuests must be at least 1")
    private Integer numberOfGuests;

    private String specialRequest;

    public UUID getHotelId() { return hotelId; }
    public void setHotelId(UUID hotelId) { this.hotelId = hotelId; }

    public UUID getRoomId() { return roomId; }
    public void setRoomId(UUID roomId) { this.roomId = roomId; }

    public LocalDate getCheckInDate() { return checkInDate; }
    public void setCheckInDate(LocalDate checkInDate) { this.checkInDate = checkInDate; }

    public LocalDate getCheckOutDate() { return checkOutDate; }
    public void setCheckOutDate(LocalDate checkOutDate) { this.checkOutDate = checkOutDate; }

    public Integer getNumberOfGuests() { return numberOfGuests; }
    public void setNumberOfGuests(Integer numberOfGuests) { this.numberOfGuests = numberOfGuests; }

    public String getSpecialRequest() { return specialRequest; }
    public void setSpecialRequest(String specialRequest) { this.specialRequest = specialRequest; }
}
