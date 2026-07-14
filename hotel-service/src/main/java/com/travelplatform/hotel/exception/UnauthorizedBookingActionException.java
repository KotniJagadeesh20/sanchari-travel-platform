package com.travelplatform.hotel.exception;

public class UnauthorizedBookingActionException extends RuntimeException {
    public UnauthorizedBookingActionException(String message) {
        super(message);
    }
}
