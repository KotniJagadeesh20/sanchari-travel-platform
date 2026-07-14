package com.travelplatform.rideshare.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.travelplatform.rideshare.client.NotificationClient;
import com.travelplatform.rideshare.entity.Ride;
import com.travelplatform.rideshare.entity.RideBooking;
import com.travelplatform.rideshare.entity.UserRef;
import com.travelplatform.rideshare.enums.BookingStatus;
import com.travelplatform.rideshare.enums.RideStatus;
import com.travelplatform.rideshare.exception.*;
import com.travelplatform.rideshare.repository.RideBookingRepository;
import com.travelplatform.rideshare.repository.RideRepository;

@ExtendWith(MockitoExtension.class)
class RideBookingServiceImplTest {

    @Mock private RideBookingRepository bookingRepo;
    @Mock private RideRepository rideRepo;
    @Mock private NotificationClient notificationClient;
    @InjectMocks private RideBookingServiceImpl bookingService;

    private UserRef driver;
    private UserRef passenger;
    private Ride ride;
    private UUID rideId;

    @BeforeEach
    void setUp() {
        driver = new UserRef(UUID.randomUUID(), "driver@example.com", "Test Driver");
        passenger = new UserRef(UUID.randomUUID(), "passenger@example.com", "Test Passenger");

        rideId = UUID.randomUUID();
        ride = new Ride();
        ride.setId(rideId);
        ride.setDriver(driver);
        ride.setStatus(RideStatus.SCHEDULED);
        ride.setTotalSeats(4);
        ride.setAvailableSeats(4);
        ride.setPricePerSeat(500.0);
    }

    // ════════════════════════════════════════════════════════════════════════
    // bookRide — business rules from spec
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    class BookRide {

        @Test
        void createsBooking_asPending_whenAllRulesPass() {
            when(rideRepo.findById(rideId)).thenReturn(Optional.of(ride));
            when(bookingRepo.save(any(RideBooking.class))).thenAnswer(i -> i.getArgument(0));

            RideBooking booking = bookingService.bookRide(rideId, 2, passenger);

            assertEquals(BookingStatus.PENDING, booking.getStatus());
            assertEquals(2, booking.getSeatsBooked());
            assertEquals(1000.0, booking.getTotalAmount());
            assertEquals(passenger, booking.getPassenger());
        }

        @Test
        void doesNotDeductAvailableSeats_atPendingTime() {
            when(rideRepo.findById(rideId)).thenReturn(Optional.of(ride));
            when(bookingRepo.save(any(RideBooking.class))).thenAnswer(i -> i.getArgument(0));

            bookingService.bookRide(rideId, 2, passenger);

            assertEquals(4, ride.getAvailableSeats(), "Seats are only deducted on approval, not at booking time");
            verify(rideRepo, never()).save(any());
        }

        @Test
        void throwsOwnRideBooking_whenDriverBooksOwnRide() {
            when(rideRepo.findById(rideId)).thenReturn(Optional.of(ride));

            assertThrows(OwnRideBookingException.class,
                    () -> bookingService.bookRide(rideId, 1, driver));

            verify(bookingRepo, never()).save(any());
        }

        @Test
        void throwsInsufficientSeats_whenRequestExceedsAvailability() {
            ride.setAvailableSeats(2);
            when(rideRepo.findById(rideId)).thenReturn(Optional.of(ride));

            assertThrows(InsufficientSeatsException.class,
                    () -> bookingService.bookRide(rideId, 3, passenger));

            verify(bookingRepo, never()).save(any());
        }

        @Test
        void throwsRideNotBookable_whenRideIsCompleted() {
            ride.setStatus(RideStatus.COMPLETED);
            when(rideRepo.findById(rideId)).thenReturn(Optional.of(ride));

            assertThrows(RideNotBookableException.class,
                    () -> bookingService.bookRide(rideId, 1, passenger));
        }

        @Test
        void throwsRideNotBookable_whenRideIsCancelled() {
            ride.setStatus(RideStatus.CANCELLED);
            when(rideRepo.findById(rideId)).thenReturn(Optional.of(ride));

            assertThrows(RideNotBookableException.class,
                    () -> bookingService.bookRide(rideId, 1, passenger));
        }

        @Test
        void throwsRideNotFound_whenRideDoesNotExist() {
            when(rideRepo.findById(rideId)).thenReturn(Optional.empty());

            assertThrows(RideNotFoundException.class,
                    () -> bookingService.bookRide(rideId, 1, passenger));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // approveBooking
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    class ApproveBooking {

        private RideBooking pendingBooking;
        private UUID bookingId;

        @BeforeEach
        void setUp() {
            bookingId = UUID.randomUUID();
            pendingBooking = new RideBooking();
            pendingBooking.setId(bookingId);
            pendingBooking.setRide(ride);
            pendingBooking.setPassenger(passenger);
            pendingBooking.setSeatsBooked(2);
            pendingBooking.setStatus(BookingStatus.PENDING);
        }

        @Test
        void approvesBooking_andDeductsSeats_whenCallerIsDriver() {
            when(bookingRepo.findById(bookingId)).thenReturn(Optional.of(pendingBooking));
            when(rideRepo.save(any(Ride.class))).thenAnswer(i -> i.getArgument(0));
            when(bookingRepo.save(any(RideBooking.class))).thenAnswer(i -> i.getArgument(0));

            RideBooking result = bookingService.approveBooking(bookingId, driver.getId());

            assertEquals(BookingStatus.APPROVED, result.getStatus());
            assertEquals(2, ride.getAvailableSeats(), "4 - 2 booked = 2 remaining");
        }

        @Test
        void throwsUnauthorized_whenCallerIsNotDriver() {
            when(bookingRepo.findById(bookingId)).thenReturn(Optional.of(pendingBooking));
            UUID strangerId = UUID.randomUUID();

            assertThrows(UnauthorizedRideActionException.class,
                    () -> bookingService.approveBooking(bookingId, strangerId));

            verify(rideRepo, never()).save(any());
        }

        @Test
        void throwsUnauthorized_whenBookingAlreadyApproved() {
            pendingBooking.setStatus(BookingStatus.APPROVED);
            when(bookingRepo.findById(bookingId)).thenReturn(Optional.of(pendingBooking));

            assertThrows(UnauthorizedRideActionException.class,
                    () -> bookingService.approveBooking(bookingId, driver.getId()));
        }

        @Test
        void throwsInsufficientSeats_whenSeatsConsumedByAnotherApprovalConcurrently() {
            ride.setAvailableSeats(1); // another approval already took most seats
            when(bookingRepo.findById(bookingId)).thenReturn(Optional.of(pendingBooking));

            assertThrows(InsufficientSeatsException.class,
                    () -> bookingService.approveBooking(bookingId, driver.getId()));
        }

        @Test
        void throwsBookingNotFound_whenBookingDoesNotExist() {
            when(bookingRepo.findById(bookingId)).thenReturn(Optional.empty());

            assertThrows(BookingNotFoundException.class,
                    () -> bookingService.approveBooking(bookingId, driver.getId()));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // rejectBooking
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    class RejectBooking {

        private RideBooking pendingBooking;
        private UUID bookingId;

        @BeforeEach
        void setUp() {
            bookingId = UUID.randomUUID();
            pendingBooking = new RideBooking();
            pendingBooking.setId(bookingId);
            pendingBooking.setRide(ride);
            pendingBooking.setPassenger(passenger);
            pendingBooking.setSeatsBooked(2);
            pendingBooking.setStatus(BookingStatus.PENDING);
        }

        @Test
        void rejectsBooking_withoutTouchingSeats() {
            when(bookingRepo.findById(bookingId)).thenReturn(Optional.of(pendingBooking));
            when(bookingRepo.save(any(RideBooking.class))).thenAnswer(i -> i.getArgument(0));

            RideBooking result = bookingService.rejectBooking(bookingId, driver.getId());

            assertEquals(BookingStatus.REJECTED, result.getStatus());
            assertEquals(4, ride.getAvailableSeats(), "Rejecting a PENDING booking never touches seats");
            verify(rideRepo, never()).save(any());
        }

        @Test
        void throwsUnauthorized_whenCallerIsNotDriver() {
            when(bookingRepo.findById(bookingId)).thenReturn(Optional.of(pendingBooking));

            assertThrows(UnauthorizedRideActionException.class,
                    () -> bookingService.rejectBooking(bookingId, UUID.randomUUID()));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // cancelBooking
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    class CancelBooking {

        private RideBooking booking;
        private UUID bookingId;

        @BeforeEach
        void setUp() {
            bookingId = UUID.randomUUID();
            booking = new RideBooking();
            booking.setId(bookingId);
            booking.setRide(ride);
            booking.setPassenger(passenger);
            booking.setSeatsBooked(2);
        }

        @Test
        void cancelsApprovedBooking_andReturnsSeats() {
            booking.setStatus(BookingStatus.APPROVED);
            ride.setAvailableSeats(2); // seats were deducted when approved
            when(bookingRepo.findById(bookingId)).thenReturn(Optional.of(booking));
            when(rideRepo.save(any(Ride.class))).thenAnswer(i -> i.getArgument(0));
            when(bookingRepo.save(any(RideBooking.class))).thenAnswer(i -> i.getArgument(0));

            bookingService.cancelBooking(bookingId, passenger.getId());

            assertEquals(BookingStatus.CANCELLED, booking.getStatus());
            assertEquals(4, ride.getAvailableSeats(), "2 returned seats: 2 + 2 = 4");
        }

        @Test
        void cancelsPendingBooking_withoutTouchingSeats() {
            booking.setStatus(BookingStatus.PENDING);
            when(bookingRepo.findById(bookingId)).thenReturn(Optional.of(booking));
            when(bookingRepo.save(any(RideBooking.class))).thenAnswer(i -> i.getArgument(0));

            bookingService.cancelBooking(bookingId, passenger.getId());

            assertEquals(BookingStatus.CANCELLED, booking.getStatus());
            verify(rideRepo, never()).save(any());
        }

        @Test
        void throwsUnauthorized_whenCallerIsNotThePassenger() {
            booking.setStatus(BookingStatus.PENDING);
            when(bookingRepo.findById(bookingId)).thenReturn(Optional.of(booking));

            assertThrows(UnauthorizedRideActionException.class,
                    () -> bookingService.cancelBooking(bookingId, driver.getId()));

            verify(bookingRepo, never()).save(any());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Reads
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    class Reads {

        @Test
        void getBookingsByPassenger_delegatesToRepository() {
            when(bookingRepo.findByPassengerId(passenger.getId())).thenReturn(List.of(new RideBooking()));
            assertEquals(1, bookingService.getBookingsByPassenger(passenger.getId()).size());
        }

        @Test
        void getBookingsByRide_returnsBookings_whenCallerIsDriver() {
            when(rideRepo.findById(rideId)).thenReturn(Optional.of(ride));
            when(bookingRepo.findByRideId(rideId)).thenReturn(List.of(new RideBooking()));

            assertEquals(1, bookingService.getBookingsByRide(rideId, driver.getId()).size());
        }

        @Test
        void getBookingsByRide_throwsUnauthorized_whenCallerIsNotDriver() {
            when(rideRepo.findById(rideId)).thenReturn(Optional.of(ride));

            assertThrows(UnauthorizedRideActionException.class,
                    () -> bookingService.getBookingsByRide(rideId, UUID.randomUUID()));

            verify(bookingRepo, never()).findByRideId(any());
        }

        @Test
        void getBookingsByRide_throwsRideNotFound_whenRideDoesNotExist() {
            when(rideRepo.findById(rideId)).thenReturn(Optional.empty());

            assertThrows(RideNotFoundException.class,
                    () -> bookingService.getBookingsByRide(rideId, driver.getId()));
        }

        @Test
        void getBookingById_throwsWhenNotFound() {
            UUID missingId = UUID.randomUUID();
            when(bookingRepo.findById(missingId)).thenReturn(Optional.empty());

            assertThrows(BookingNotFoundException.class, () -> bookingService.getBookingById(missingId));
        }
    }
}
