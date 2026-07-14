package com.travelplatform.packages.destination.exception;

import java.util.UUID;

public class DestinationNotFoundException extends RuntimeException {
    public DestinationNotFoundException(UUID destinationId) {
        super("Destination not found: " + destinationId);
    }
}
