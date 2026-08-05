package com.travelplatform.rideshare.service;

import com.travelplatform.rideshare.dto.CreateRideRequest;
import com.travelplatform.rideshare.dto.UpdateRideRequest;
import com.travelplatform.rideshare.entity.Ride;
import com.travelplatform.rideshare.entity.UserRef;
import com.travelplatform.rideshare.enums.RideStatus;
import com.travelplatform.rideshare.exception.RideNotFoundException;
import com.travelplatform.rideshare.exception.UnauthorizedRideActionException;
import com.travelplatform.rideshare.repository.RideRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class RideServiceImpl implements RideService {

    @Autowired
    private RideRepository rideRepo;

    @Override
    @Transactional
    public Ride createRide(CreateRideRequest request, UserRef driver) {
        Ride ride = new Ride();
        ride.setSource(request.getSource());
        ride.setDestination(request.getDestination());
        ride.setTravelDate(request.getTravelDate());
        ride.setDepartureTime(request.getDepartureTime());
        ride.setTotalSeats(request.getTotalSeats());
        ride.setAvailableSeats(request.getTotalSeats()); // fully available on creation
        ride.setPricePerSeat(request.getPricePerSeat());
        ride.setVehicleType(request.getVehicleType());
        ride.setVehicleNumber(request.getVehicleNumber());
        ride.setPickupPoint(request.getPickupPoint());
        ride.setDropPoint(request.getDropPoint());
        ride.setStatus(RideStatus.SCHEDULED);
        ride.setDriver(driver);
        return rideRepo.save(ride);
    }

    @Override
    @Transactional
    public Ride updateRide(UUID rideId, UpdateRideRequest request, UUID callerId) {
        Ride ride = getRideById(rideId);
        assertOwnership(ride, callerId, "update");

        if (request.getTravelDate() != null) ride.setTravelDate(request.getTravelDate());
        if (request.getDepartureTime() != null) ride.setDepartureTime(request.getDepartureTime());
        if (request.getPricePerSeat() != null) ride.setPricePerSeat(request.getPricePerSeat());
        if (request.getVehicleType() != null) ride.setVehicleType(request.getVehicleType());
        if (request.getVehicleNumber() != null) ride.setVehicleNumber(request.getVehicleNumber());
        if (request.getPickupPoint() != null) ride.setPickupPoint(request.getPickupPoint());
        if (request.getDropPoint() != null) ride.setDropPoint(request.getDropPoint());

        if (request.getTotalSeats() != null) {
            int delta = request.getTotalSeats() - ride.getTotalSeats();
            ride.setTotalSeats(request.getTotalSeats());
            // Shift availableSeats by the same delta so already-booked seats stay booked.
            ride.setAvailableSeats(Math.max(0, ride.getAvailableSeats() + delta));
        }

        return rideRepo.save(ride);
    }

    @Override
    @Transactional
    public void cancelRide(UUID rideId, UUID callerId) {
        Ride ride = getRideById(rideId);
        assertOwnership(ride, callerId, "cancel");
        ride.setStatus(RideStatus.CANCELLED);
        rideRepo.save(ride);
    }

    @Override
    public List<Ride> searchRides(String source, String destination, LocalDate travelDate) {
        return rideRepo.findBySourceAndDestinationAndTravelDateAndStatus(
                source, destination, travelDate, RideStatus.SCHEDULED);
    }

    @Override
    public List<Ride> getRidesByDriver(UUID driverId) {
        return rideRepo.findByDriverId(driverId);
    }

    @Override
    public Ride getRideById(UUID rideId) {
        return rideRepo.findById(rideId).orElseThrow(() -> new RideNotFoundException(rideId));
    }

    private void assertOwnership(Ride ride, UUID callerId, String action) {
        if (!ride.getDriver().getId().equals(callerId)) {
            throw new UnauthorizedRideActionException(
                    "Only the ride's driver can " + action + " this ride.");
        }
    }
}
