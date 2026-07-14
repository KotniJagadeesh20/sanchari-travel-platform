package com.travelplatform.rideshare.exception;

/** Thrown when a driver attempts to book a seat on their own ride. */
public class OwnRideBookingException extends RuntimeException {
    public OwnRideBookingException() {
        super("You cannot book a seat on your own ride.");
    }
}
