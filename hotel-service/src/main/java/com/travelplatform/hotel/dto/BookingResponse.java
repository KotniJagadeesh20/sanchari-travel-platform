package com.travelplatform.hotel.dto;

import com.travelplatform.hotel.entity.HotelBooking;
import com.travelplatform.hotel.enums.BookingStatus;
import com.travelplatform.hotel.enums.PaymentStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class BookingResponse {

    private UUID id;
    private UUID userId;
    private UUID hotelId;
    private String hotelName;
    private UUID roomId;
    private String roomNumber;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer numberOfGuests;
    private Double pricePerNight;
    private Double totalAmount;
    private BookingStatus bookingStatus;
    private PaymentStatus paymentStatus;
    private String specialRequest;
    private LocalDateTime bookingDate;

    public static BookingResponse from(HotelBooking b) {
        BookingResponse r = new BookingResponse();
        r.id = b.getId();
        r.userId = b.getUserId();
        r.hotelId = b.getHotel().getId();
        r.hotelName = b.getHotel().getName();
        r.roomId = b.getRoom().getId();
        r.roomNumber = b.getRoom().getRoomNumber();
        r.checkInDate = b.getCheckInDate();
        r.checkOutDate = b.getCheckOutDate();
        r.numberOfGuests = b.getNumberOfGuests();
        r.pricePerNight = b.getPricePerNight();
        r.totalAmount = b.getTotalAmount();
        r.bookingStatus = b.getBookingStatus();
        r.paymentStatus = b.getPaymentStatus();
        r.specialRequest = b.getSpecialRequest();
        r.bookingDate = b.getBookingDate();
        return r;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getHotelId() { return hotelId; }
    public String getHotelName() { return hotelName; }
    public UUID getRoomId() { return roomId; }
    public String getRoomNumber() { return roomNumber; }
    public LocalDate getCheckInDate() { return checkInDate; }
    public LocalDate getCheckOutDate() { return checkOutDate; }
    public Integer getNumberOfGuests() { return numberOfGuests; }
    public Double getPricePerNight() { return pricePerNight; }
    public Double getTotalAmount() { return totalAmount; }
    public BookingStatus getBookingStatus() { return bookingStatus; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public String getSpecialRequest() { return specialRequest; }
    public LocalDateTime getBookingDate() { return bookingDate; }
}
