package com.travelplatform.busbooking.exception;

/**
 * Thrown when an authenticated user attempts to act on a booking they do not own
 * (e.g. cancelling someone else's bus ticket).
 */
public class UnauthorizedBookingActionException extends RuntimeException {
    public UnauthorizedBookingActionException(String message) {
        super(message);
    }
}
