package com.travelplatform.hotel.exception;

import java.util.UUID;

public class BookingNotFoundException extends RuntimeException {
    public BookingNotFoundException(UUID bookingId) {
        super("Hotel booking not found with id: " + bookingId);
    }
}
