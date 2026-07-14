package com.travelplatform.hotel.service;

import com.travelplatform.hotel.dto.CreateBookingRequest;
import com.travelplatform.hotel.entity.HotelBooking;

import java.util.List;
import java.util.UUID;

public interface HotelBookingService {

    /**
     * recipientEmail is the caller's own email, taken from the gateway-forwarded
     * X-Authenticated-Email header — used only to pass through to the booking
     * confirmation notification. Never null in practice (the gateway always sets
     * it once a request is authenticated), but callers should treat it as optional.
     */
    HotelBooking bookHotel(CreateBookingRequest request, UUID userId, String recipientEmail);

    void cancelBooking(UUID bookingId, UUID callerId, String recipientEmail);

    List<HotelBooking> getBookingsByUser(UUID userId);

    HotelBooking getBookingById(UUID bookingId);
}
