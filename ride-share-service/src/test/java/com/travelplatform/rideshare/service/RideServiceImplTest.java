package com.travelplatform.rideshare.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalTime;
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

import com.travelplatform.rideshare.dto.CreateRideRequest;
import com.travelplatform.rideshare.dto.UpdateRideRequest;
import com.travelplatform.rideshare.entity.Ride;
import com.travelplatform.rideshare.entity.UserRef;
import com.travelplatform.rideshare.enums.RideStatus;
import com.travelplatform.rideshare.exception.RideNotFoundException;
import com.travelplatform.rideshare.exception.UnauthorizedRideActionException;
import com.travelplatform.rideshare.repository.RideRepository;

@ExtendWith(MockitoExtension.class)
class RideServiceImplTest {

    @Mock private RideRepository rideRepo;
    @InjectMocks private RideServiceImpl rideService;

    private UserRef driver;
    private CreateRideRequest createRequest;

    @BeforeEach
    void setUp() {
        driver = new UserRef(UUID.randomUUID(), "driver@example.com", "Test Driver");

        createRequest = new CreateRideRequest();
        createRequest.setSource("Hyderabad");
        createRequest.setDestination("Vijayawada");
        createRequest.setTravelDate(LocalDate.now().plusDays(3));
        createRequest.setDepartureTime(LocalTime.of(8, 0));
        createRequest.setTotalSeats(4);
        createRequest.setPricePerSeat(500.0);
    }

    @Nested
    class CreateRide {

        @Test
        void createsRideWithFullAvailability_andScheduledStatus() {
            when(rideRepo.save(any(Ride.class))).thenAnswer(i -> i.getArgument(0));

            Ride ride = rideService.createRide(createRequest, driver);

            assertEquals(4, ride.getAvailableSeats(), "New ride starts with all seats available");
            assertEquals(RideStatus.SCHEDULED, ride.getStatus());
            assertEquals(driver, ride.getDriver());
        }
    }

    @Nested
    class UpdateRide {

        private Ride existingRide;
        private UUID rideId;

        @BeforeEach
        void setUp() {
            rideId = UUID.randomUUID();
            existingRide = new Ride();
            existingRide.setId(rideId);
            existingRide.setDriver(driver);
            existingRide.setTotalSeats(4);
            existingRide.setAvailableSeats(4);
            existingRide.setPricePerSeat(500.0);
            existingRide.setStatus(RideStatus.SCHEDULED);
        }

        @Test
        void updatesFields_whenCallerIsDriver() {
            when(rideRepo.findById(rideId)).thenReturn(Optional.of(existingRide));
            when(rideRepo.save(any(Ride.class))).thenAnswer(i -> i.getArgument(0));

            UpdateRideRequest req = new UpdateRideRequest();
            req.setPricePerSeat(600.0);

            Ride updated = rideService.updateRide(rideId, req, driver.getId());

            assertEquals(600.0, updated.getPricePerSeat());
        }

        @Test
        void throwsUnauthorized_whenCallerIsNotDriver() {
            when(rideRepo.findById(rideId)).thenReturn(Optional.of(existingRide));
            UUID strangerId = UUID.randomUUID();

            assertThrows(UnauthorizedRideActionException.class,
                    () -> rideService.updateRide(rideId, new UpdateRideRequest(), strangerId));

            verify(rideRepo, never()).save(any());
        }

        @Test
        void throwsRideNotFound_whenRideDoesNotExist() {
            when(rideRepo.findById(rideId)).thenReturn(Optional.empty());

            assertThrows(RideNotFoundException.class,
                    () -> rideService.updateRide(rideId, new UpdateRideRequest(), driver.getId()));
        }

        @Test
        void shiftsAvailableSeats_whenTotalSeatsIncreased() {
            existingRide.setTotalSeats(4);
            existingRide.setAvailableSeats(2); // 2 already booked
            when(rideRepo.findById(rideId)).thenReturn(Optional.of(existingRide));
            when(rideRepo.save(any(Ride.class))).thenAnswer(i -> i.getArgument(0));

            UpdateRideRequest req = new UpdateRideRequest();
            req.setTotalSeats(6); // +2 seats added

            Ride updated = rideService.updateRide(rideId, req, driver.getId());

            assertEquals(6, updated.getTotalSeats());
            assertEquals(4, updated.getAvailableSeats(), "2 already-booked seats stay booked; +2 new seats become available");
        }
    }

    @Nested
    class CancelRide {

        private Ride existingRide;
        private UUID rideId;

        @BeforeEach
        void setUp() {
            rideId = UUID.randomUUID();
            existingRide = new Ride();
            existingRide.setId(rideId);
            existingRide.setDriver(driver);
            existingRide.setStatus(RideStatus.SCHEDULED);
        }

        @Test
        void setsCancelledStatus_whenCallerIsDriver() {
            when(rideRepo.findById(rideId)).thenReturn(Optional.of(existingRide));
            when(rideRepo.save(any(Ride.class))).thenAnswer(i -> i.getArgument(0));

            rideService.cancelRide(rideId, driver.getId());

            assertEquals(RideStatus.CANCELLED, existingRide.getStatus());
        }

        @Test
        void throwsUnauthorized_whenCallerIsNotDriver() {
            when(rideRepo.findById(rideId)).thenReturn(Optional.of(existingRide));
            UUID strangerId = UUID.randomUUID();

            assertThrows(UnauthorizedRideActionException.class,
                    () -> rideService.cancelRide(rideId, strangerId));

            verify(rideRepo, never()).save(any());
        }
    }

    @Nested
    class SearchAndLookup {

        @Test
        void searchRides_onlyReturnsScheduledStatus() {
            LocalDate date = LocalDate.now().plusDays(1);
            when(rideRepo.findBySourceAndDestinationAndTravelDateAndStatus(
                    "Hyderabad", "Vijayawada", date, RideStatus.SCHEDULED))
                    .thenReturn(List.of(new Ride()));

            List<Ride> results = rideService.searchRides("Hyderabad", "Vijayawada", date);

            assertEquals(1, results.size());
            verify(rideRepo).findBySourceAndDestinationAndTravelDateAndStatus(
                    "Hyderabad", "Vijayawada", date, RideStatus.SCHEDULED);
        }

        @Test
        void getRideById_throwsWhenNotFound() {
            UUID missingId = UUID.randomUUID();
            when(rideRepo.findById(missingId)).thenReturn(Optional.empty());

            assertThrows(RideNotFoundException.class, () -> rideService.getRideById(missingId));
        }

        @Test
        void getRidesByDriver_delegatesToRepository() {
            when(rideRepo.findByDriverId(driver.getId())).thenReturn(List.of(new Ride()));

            List<Ride> results = rideService.getRidesByDriver(driver.getId());

            assertEquals(1, results.size());
        }
    }
}
