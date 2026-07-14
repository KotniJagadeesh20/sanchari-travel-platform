package com.travelplatform.rideshare.service;

import com.travelplatform.rideshare.entity.RideBooking;
import com.travelplatform.rideshare.entity.UserRef;

import java.util.List;
import java.util.UUID;

public interface RideBookingService {

    /**
     * Books seats on a ride as PENDING (awaiting driver approval).
     * Enforces: driver cannot book own ride, ride must be SCHEDULED,
     * requested seats must not exceed currently available seats.
     * Seats are NOT deducted from availableSeats until the driver approves —
     * see {@link #approveBooking}.
     */
    RideBooking bookRide(UUID rideId, Integer seats, UserRef passenger);

    /**
     * Driver approves a PENDING booking. Deducts seats from the ride's
     * availableSeats. Throws UnauthorizedRideActionException if the caller
     * is not the ride's driver, InsufficientSeatsException if seats were
     * consumed by another approval in the meantime.
     */
    RideBooking approveBooking(UUID bookingId, UUID callerId);

    /**
     * Driver rejects a PENDING booking. No seats are deducted (none were
     * deducted at PENDING time).
     */
    RideBooking rejectBooking(UUID bookingId, UUID callerId);

    /**
     * Passenger cancels their own booking. If it was APPROVED, the seats
     * are returned to the ride's availableSeats.
     */
    void cancelBooking(UUID bookingId, UUID callerId);

    List<RideBooking> getBookingsByPassenger(UUID passengerId);

    /**
     * Driver-only view of bookings on one of their rides.
     * Throws UnauthorizedRideActionException if the caller does not own the ride.
     */
    List<RideBooking> getBookingsByRide(UUID rideId, UUID callerId);

    RideBooking getBookingById(UUID bookingId);
}
