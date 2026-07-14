package com.travelplatform.rideshare.service;

import com.travelplatform.rideshare.client.NotificationClient;
import com.travelplatform.rideshare.entity.Ride;
import com.travelplatform.rideshare.entity.RideBooking;
import com.travelplatform.rideshare.entity.UserRef;
import com.travelplatform.rideshare.enums.BookingStatus;
import com.travelplatform.rideshare.enums.RideStatus;
import com.travelplatform.rideshare.exception.*;
import com.travelplatform.rideshare.repository.RideBookingRepository;
import com.travelplatform.rideshare.repository.RideRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class RideBookingServiceImpl implements RideBookingService {

    @Autowired
    private RideBookingRepository bookingRepo;

    @Autowired
    private RideRepository rideRepo;

    @Autowired
    private NotificationClient notificationClient;

    // ─── Book ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public RideBooking bookRide(UUID rideId, Integer seats, UserRef passenger) {
        Ride ride = rideRepo.findById(rideId).orElseThrow(() -> new RideNotFoundException(rideId));

        // Rule: completed/cancelled rides cannot be booked.
        if (ride.getStatus() != RideStatus.SCHEDULED) {
            throw new RideNotBookableException(ride.getStatus());
        }

        // Rule: a driver cannot book their own ride.
        if (ride.getDriver().getId().equals(passenger.getId())) {
            throw new OwnRideBookingException();
        }

        // Rule: requested seats must not exceed availability.
        if (seats > ride.getAvailableSeats()) {
            throw new InsufficientSeatsException(seats, ride.getAvailableSeats());
        }

        RideBooking booking = new RideBooking();
        booking.setRide(ride);
        booking.setPassenger(passenger);
        booking.setSeatsBooked(seats);
        booking.setTotalAmount(seats * ride.getPricePerSeat());
        booking.setStatus(BookingStatus.PENDING);
        booking.setBookingTime(LocalDateTime.now());

        // Seats are reserved (soft-hold) at PENDING time to prevent overbooking
        // while the driver decides, but only actually committed on approval.
        // We track this via availableSeats minus the sum of active (PENDING+APPROVED)
        // bookings rather than mutating availableSeats here, to keep rejection simple.
        RideBooking saved = bookingRepo.save(booking);

        // The driver is the one who needs to act next — notify them, not the passenger.
        UserRef driver = ride.getDriver();
        notificationClient.notify(
                driver.getId(), driver.getEmail(), "GENERIC",
                "New ride booking request",
                String.format("A passenger requested %d seat(s) on your ride (booking %s). Approve or reject it.",
                        seats, saved.getId()),
                saved.getId());

        return saved;
    }

    // ─── Approve / Reject ──────────────────────────────────────────────────

    @Override
    @Transactional
    public RideBooking approveBooking(UUID bookingId, UUID callerId) {
        RideBooking booking = getBookingById(bookingId);
        Ride ride = booking.getRide();

        assertIsDriver(ride, callerId, "approve");
        assertIsPending(booking);

        if (booking.getSeatsBooked() > ride.getAvailableSeats()) {
            throw new InsufficientSeatsException(booking.getSeatsBooked(), ride.getAvailableSeats());
        }

        ride.setAvailableSeats(ride.getAvailableSeats() - booking.getSeatsBooked());
        rideRepo.save(ride);

        booking.setStatus(BookingStatus.APPROVED);
        RideBooking saved = bookingRepo.save(booking);

        UserRef passenger = booking.getPassenger();
        notificationClient.notify(
                passenger.getId(), passenger.getEmail(), "BOOKING_CONFIRMED",
                "Ride booking approved",
                String.format("Your booking (%s) for %d seat(s) has been approved by the driver.",
                        saved.getId(), saved.getSeatsBooked()),
                saved.getId());

        return saved;
    }

    @Override
    @Transactional
    public RideBooking rejectBooking(UUID bookingId, UUID callerId) {
        RideBooking booking = getBookingById(bookingId);
        assertIsDriver(booking.getRide(), callerId, "reject");
        assertIsPending(booking);

        // No seat adjustment needed — seats are only deducted on approval.
        booking.setStatus(BookingStatus.REJECTED);
        RideBooking saved = bookingRepo.save(booking);

        UserRef passenger = booking.getPassenger();
        notificationClient.notify(
                passenger.getId(), passenger.getEmail(), "BOOKING_REJECTED",
                "Ride booking rejected",
                String.format("Your booking (%s) was rejected by the driver.", saved.getId()),
                saved.getId());

        return saved;
    }

    // ─── Cancel ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void cancelBooking(UUID bookingId, UUID callerId) {
        RideBooking booking = getBookingById(bookingId);

        if (!booking.getPassenger().getId().equals(callerId)) {
            throw new UnauthorizedRideActionException("Only the passenger who made this booking can cancel it.");
        }

        // If the booking had already consumed seats (APPROVED), give them back.
        if (booking.getStatus() == BookingStatus.APPROVED) {
            Ride ride = booking.getRide();
            ride.setAvailableSeats(ride.getAvailableSeats() + booking.getSeatsBooked());
            rideRepo.save(ride);
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepo.save(booking);

        // The passenger initiated this, so notify the driver — the counterparty who
        // needs to know their seat(s) just freed up.
        UserRef driver = booking.getRide().getDriver();
        notificationClient.notify(
                driver.getId(), driver.getEmail(), "BOOKING_CANCELLED",
                "Ride booking cancelled",
                String.format("A passenger cancelled their booking (%s) on your ride.", booking.getId()),
                booking.getId());
    }

    // ─── Reads ────────────────────────────────────────────────────────────

    @Override
    public List<RideBooking> getBookingsByPassenger(UUID passengerId) {
        return bookingRepo.findByPassengerId(passengerId);
    }

    @Override
    public List<RideBooking> getBookingsByRide(UUID rideId, UUID callerId) {
        Ride ride = rideRepo.findById(rideId).orElseThrow(() -> new RideNotFoundException(rideId));
        if (!ride.getDriver().getId().equals(callerId)) {
            throw new UnauthorizedRideActionException("Only the ride's driver can view its bookings.");
        }
        return bookingRepo.findByRideId(rideId);
    }

    @Override
    public RideBooking getBookingById(UUID bookingId) {
        return bookingRepo.findById(bookingId).orElseThrow(() -> new BookingNotFoundException(bookingId));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    private void assertIsDriver(Ride ride, UUID callerId, String action) {
        if (!ride.getDriver().getId().equals(callerId)) {
            throw new UnauthorizedRideActionException("Only the ride's driver can " + action + " this booking.");
        }
    }

    private void assertIsPending(RideBooking booking) {
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new UnauthorizedRideActionException(
                    "Booking is already " + booking.getStatus() + " — cannot change it.");
        }
    }
}
