package com.travelplatform.rideshare.exception;

/** Thrown when a user attempts an action on a ride/booking they don't own (e.g. approving someone else's booking). */
public class UnauthorizedRideActionException extends RuntimeException {
    public UnauthorizedRideActionException(String message) {
        super(message);
    }
}
