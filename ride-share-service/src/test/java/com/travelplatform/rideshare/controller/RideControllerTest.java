package com.travelplatform.rideshare.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.travelplatform.rideshare.entity.Ride;
import com.travelplatform.rideshare.entity.RideBooking;
import com.travelplatform.rideshare.entity.UserRef;
import com.travelplatform.rideshare.enums.BookingStatus;
import com.travelplatform.rideshare.enums.RideStatus;
import com.travelplatform.rideshare.exception.GlobalExceptionHandler;
import com.travelplatform.rideshare.exception.OwnRideBookingException;
import com.travelplatform.rideshare.exception.UnauthorizedRideActionException;
import com.travelplatform.rideshare.service.RideBookingService;
import com.travelplatform.rideshare.service.RideService;
import com.travelplatform.rideshare.service.UserRefService;

@ExtendWith(MockitoExtension.class)
class RideControllerTest {

    @Mock private RideService rideService;
    @Mock private RideBookingService bookingService;
    @Mock private UserRefService userRefService;
    @InjectMocks private RideController rideController;

    private MockMvc mockMvc;

    private UUID driverId;
    private UUID passengerId;
    private String driverEmail = "driver@example.com";
    private String passengerEmail = "passenger@example.com";
    private String driverName = "Test Driver";
    private String passengerName = "Test Passenger";

    private static final String CREATE_RIDE_JSON = """
            {"source":"Hyderabad","destination":"Vijayawada","travelDate":"2026-06-25",
             "departureTime":"08:00","totalSeats":4,"pricePerSeat":500.0}
            """;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(rideController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(new LocalValidatorFactoryBean())
                .build();

        driverId = UUID.randomUUID();
        passengerId = UUID.randomUUID();
    }

    private Ride buildRide(UUID rideId, UserRef driver) {
        Ride ride = new Ride();
        ride.setId(rideId);
        ride.setSource("Hyderabad");
        ride.setDestination("Vijayawada");
        ride.setTravelDate(LocalDate.now().plusDays(3));
        ride.setDepartureTime(LocalTime.of(8, 0));
        ride.setTotalSeats(4);
        ride.setAvailableSeats(4);
        ride.setPricePerSeat(500.0);
        ride.setStatus(RideStatus.SCHEDULED);
        ride.setDriver(driver);
        return ride;
    }

    @Nested
    class CreateRide {

        @Test
        void returns201_withRideDetails() throws Exception {
            UserRef driver = new UserRef(driverId, driverEmail, driverName);
            when(userRefService.findOrCreate(driverId, driverEmail, driverName)).thenReturn(driver);
            when(rideService.createRide(any(), eq(driver))).thenReturn(buildRide(UUID.randomUUID(), driver));

            mockMvc.perform(post("/rides")
                    .header("X-Authenticated-Email", driverEmail)
                    .header("X-Authenticated-Name", driverName)
                    .header("X-Authenticated-User-Id", driverId.toString())
                    .contentType("application/json").content(CREATE_RIDE_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.source", is("Hyderabad")))
                    .andExpect(jsonPath("$.availableSeats", is(4)));
        }

        @Test
        void returns400_whenSourceMissing() throws Exception {
            String invalid = CREATE_RIDE_JSON.replace("\"source\":\"Hyderabad\",", "");

            mockMvc.perform(post("/rides")
                    .header("X-Authenticated-Email", driverEmail)
                    .header("X-Authenticated-Name", driverName)
                    .header("X-Authenticated-User-Id", driverId.toString())
                    .contentType("application/json").content(invalid))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.source").exists());

            verifyNoInteractions(rideService);
        }
    }

    @Nested
    class SearchRides {

        @Test
        void returns200_withMatchingRides() throws Exception {
            UserRef driver = new UserRef(driverId, driverEmail, driverName);
            when(rideService.searchRides(eq("Hyderabad"), eq("Vijayawada"), any()))
                    .thenReturn(List.of(buildRide(UUID.randomUUID(), driver)));

            mockMvc.perform(get("/rides/search")
                    .param("source", "Hyderabad")
                    .param("destination", "Vijayawada")
                    .param("date", "2026-06-25"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].source", is("Hyderabad")));
        }
    }

    @Nested
    class BookRide {

        private static final String BOOK_JSON = "{\"seats\":2}";

        @Test
        void returns201_withPendingBooking() throws Exception {
            UUID rideId = UUID.randomUUID();
            UserRef driver = new UserRef(driverId, driverEmail, driverName);
            UserRef passenger = new UserRef(passengerId, passengerEmail, passengerName);
            Ride ride = buildRide(rideId, driver);

            when(userRefService.findOrCreate(passengerId, passengerEmail, passengerName)).thenReturn(passenger);

            RideBooking booking = new RideBooking();
            booking.setId(UUID.randomUUID());
            booking.setRide(ride);
            booking.setPassenger(passenger);
            booking.setSeatsBooked(2);
            booking.setTotalAmount(1000.0);
            booking.setStatus(BookingStatus.PENDING);
            booking.setBookingTime(java.time.LocalDateTime.now());

            when(bookingService.bookRide(rideId, 2, passenger)).thenReturn(booking);

            mockMvc.perform(post("/rides/" + rideId + "/book")
                    .header("X-Authenticated-Email", passengerEmail)
                    .header("X-Authenticated-Name", passengerName)
                    .header("X-Authenticated-User-Id", passengerId.toString())
                    .contentType("application/json").content(BOOK_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status", is("PENDING")))
                    .andExpect(jsonPath("$.seatsBooked", is(2)));
        }

        @Test
        void returns400_whenDriverBooksOwnRide() throws Exception {
            UUID rideId = UUID.randomUUID();
            UserRef driver = new UserRef(driverId, driverEmail, driverName);
            when(userRefService.findOrCreate(driverId, driverEmail, driverName)).thenReturn(driver);
            when(bookingService.bookRide(eq(rideId), eq(2), eq(driver)))
                    .thenThrow(new OwnRideBookingException());

            mockMvc.perform(post("/rides/" + rideId + "/book")
                    .header("X-Authenticated-Email", driverEmail)
                    .header("X-Authenticated-Name", driverName)
                    .header("X-Authenticated-User-Id", driverId.toString())
                    .contentType("application/json").content(BOOK_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("own ride")));
        }

        @Test
        void returns400_whenSeatsMissingOrZero() throws Exception {
            UUID rideId = UUID.randomUUID();

            mockMvc.perform(post("/rides/" + rideId + "/book")
                    .header("X-Authenticated-Email", passengerEmail)
                    .header("X-Authenticated-Name", passengerName)
                    .header("X-Authenticated-User-Id", passengerId.toString())
                    .contentType("application/json").content("{\"seats\":0}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(bookingService);
        }
    }

    @Nested
    class ApproveReject {

        @Test
        void approve_returns200_withApprovedStatus() throws Exception {
            UUID bookingId = UUID.randomUUID();
            UserRef driver = new UserRef(driverId, driverEmail, driverName);
            UserRef passenger = new UserRef(passengerId, passengerEmail, passengerName);
            Ride ride = buildRide(UUID.randomUUID(), driver);

            RideBooking approved = new RideBooking();
            approved.setId(bookingId);
            approved.setRide(ride);
            approved.setPassenger(passenger);
            approved.setSeatsBooked(2);
            approved.setTotalAmount(1000.0);
            approved.setStatus(BookingStatus.APPROVED);
            approved.setBookingTime(java.time.LocalDateTime.now());

            when(bookingService.approveBooking(bookingId, driverId)).thenReturn(approved);

            mockMvc.perform(post("/rides/bookings/" + bookingId + "/approve")
                    .header("X-Authenticated-User-Id", driverId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("APPROVED")));
        }

        @Test
        void approve_returns403_whenCallerNotDriver() throws Exception {
            UUID bookingId = UUID.randomUUID();
            UUID strangerId = UUID.randomUUID();
            when(bookingService.approveBooking(bookingId, strangerId))
                    .thenThrow(new UnauthorizedRideActionException("Only the ride's driver can approve this booking."));

            mockMvc.perform(post("/rides/bookings/" + bookingId + "/approve")
                    .header("X-Authenticated-User-Id", strangerId.toString()))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class CancelRide {

        @Test
        void returns204_whenCancelledByDriver() throws Exception {
            UUID rideId = UUID.randomUUID();

            mockMvc.perform(delete("/rides/" + rideId)
                    .header("X-Authenticated-User-Id", driverId.toString()))
                    .andExpect(status().isNoContent());

            verify(rideService).cancelRide(rideId, driverId);
        }
    }

    @Nested
    class GetBookingsForRide {

        @Test
        void returns200_whenCallerIsDriver() throws Exception {
            UUID rideId = UUID.randomUUID();
            UserRef driver = new UserRef(driverId, driverEmail, driverName);
            UserRef passenger = new UserRef(passengerId, passengerEmail, passengerName);
            Ride ride = buildRide(rideId, driver);

            RideBooking booking = new RideBooking();
            booking.setId(UUID.randomUUID());
            booking.setRide(ride);
            booking.setPassenger(passenger);
            booking.setSeatsBooked(1);
            booking.setTotalAmount(500.0);
            booking.setStatus(BookingStatus.PENDING);
            booking.setBookingTime(java.time.LocalDateTime.now());

            when(bookingService.getBookingsByRide(rideId, driverId)).thenReturn(List.of(booking));

            mockMvc.perform(get("/rides/" + rideId + "/bookings")
                    .header("X-Authenticated-User-Id", driverId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].status", is("PENDING")));
        }

        @Test
        void returns403_whenCallerIsNotDriver() throws Exception {
            UUID rideId = UUID.randomUUID();
            UUID strangerId = UUID.randomUUID();
            when(bookingService.getBookingsByRide(rideId, strangerId))
                    .thenThrow(new UnauthorizedRideActionException("Only the ride's driver can view its bookings."));

            mockMvc.perform(get("/rides/" + rideId + "/bookings")
                    .header("X-Authenticated-User-Id", strangerId.toString()))
                    .andExpect(status().isForbidden());
        }
    }
}
