package com.travelplatform.rideshare.exception;

public class InsufficientSeatsException extends RuntimeException {
    public InsufficientSeatsException(int requested, int available) {
        super("Requested " + requested + " seat(s) but only " + available + " available.");
    }
}
