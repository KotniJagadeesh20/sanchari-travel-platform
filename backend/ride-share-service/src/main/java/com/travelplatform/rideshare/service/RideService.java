package com.travelplatform.rideshare.service;

import com.travelplatform.rideshare.dto.CreateRideRequest;
import com.travelplatform.rideshare.dto.UpdateRideRequest;
import com.travelplatform.rideshare.entity.Ride;
import com.travelplatform.rideshare.entity.UserRef;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface RideService {

    /** Creates a new ride owned by the given driver. */
    Ride createRide(CreateRideRequest request, UserRef driver);

    /**
     * Updates a ride. Throws UnauthorizedRideActionException if the caller
     * is not the ride's driver.
     */
    Ride updateRide(UUID rideId, UpdateRideRequest request, UUID callerId);

    /**
     * Cancels a ride (sets status to CANCELLED). Throws UnauthorizedRideActionException
     * if the caller is not the ride's driver. Does NOT delete booking history.
     */
    void cancelRide(UUID rideId, UUID callerId);

    /** Search rides matching route + date that are still SCHEDULED (bookable). */
    List<Ride> searchRides(String source, String destination, LocalDate travelDate);

    /** All rides created by a given driver, regardless of status. */
    List<Ride> getRidesByDriver(UUID driverId);

    Ride getRideById(UUID rideId);
}
