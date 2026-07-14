package com.travelplatform.packages.exception;

public class InsufficientSlotsException extends RuntimeException {
    public InsufficientSlotsException(int requested, int available) {
        super("Requested " + requested + " traveler slot(s) but only " + available + " available.");
    }
}
