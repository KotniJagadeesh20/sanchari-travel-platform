package com.travelplatform.rideshare.exception;

import com.travelplatform.rideshare.enums.RideStatus;

/** Thrown when attempting to book/modify a ride that is COMPLETED or CANCELLED. */
public class RideNotBookableException extends RuntimeException {
    public RideNotBookableException(RideStatus status) {
        super("Ride cannot be booked — current status is " + status);
    }
}
