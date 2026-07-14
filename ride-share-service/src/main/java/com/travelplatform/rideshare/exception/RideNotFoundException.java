package com.travelplatform.rideshare.exception;

import java.util.UUID;

public class RideNotFoundException extends RuntimeException {
    public RideNotFoundException(UUID rideId) {
        super("Ride not found: " + rideId);
    }
}
