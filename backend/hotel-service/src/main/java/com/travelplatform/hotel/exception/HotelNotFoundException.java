package com.travelplatform.hotel.exception;

import java.util.UUID;

public class HotelNotFoundException extends RuntimeException {
    public HotelNotFoundException(UUID hotelId) {
        super("Hotel not found with id: " + hotelId);
    }
}
