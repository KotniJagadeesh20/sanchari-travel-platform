package com.travelplatform.packages.exception;

/** Thrown when a user attempts to act on a booking they don't own (e.g. cancelling someone else's booking). */
public class UnauthorizedBookingActionException extends RuntimeException {
    public UnauthorizedBookingActionException(String message) {
        super(message);
    }
}
