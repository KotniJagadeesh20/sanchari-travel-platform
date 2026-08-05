package com.travelplatform.packages.service;

import com.travelplatform.packages.dto.TravelerRequest;
import com.travelplatform.packages.entity.PackageBooking;
import com.travelplatform.packages.entity.UserRef;

import java.util.List;
import java.util.UUID;

public interface PackageBookingService {

    /**
     * Books a specific departure batch for the given traveler. Auto-confirms
     * immediately (no approval step — packages are admin-curated inventory,
     * not peer-to-peer). Enforces: the departure must be active and belong to
     * an active package, and travelers.size() must not exceed the
     * departure's availableSlots. Deducts availableSlots immediately since
     * the booking is confirmed on creation. paymentStatus starts PENDING —
     * no payment gateway is integrated yet.
     */
    PackageBooking bookPackage(UUID departureId, List<TravelerRequest> travelers, UserRef traveler);

    /**
     * Traveler cancels their own booking. If it was CONFIRMED, the slots
     * are returned to the departure's availableSlots.
     */
    void cancelBooking(UUID bookingId, UUID callerId, String reason);

    /**
     * Admin/partner cancels a customer's booking (e.g. trip called off).
     * No ownership check — same as every other admin-scoped action in this
     * service today; restricting this to the package's own creator is a
     * ROLE_PARTNER-era enforcement, not something added here.
     */
    void cancelBookingAsAdmin(UUID bookingId, String reason);

    List<PackageBooking> getBookingsByTraveler(UUID travelerId);

    /** Admin-only view of all bookings on a given package, across all its departures. */
    List<PackageBooking> getBookingsByPackage(UUID packageId);

    PackageBooking getBookingById(UUID bookingId);
}
