package com.travelplatform.packages.exception;

import java.util.UUID;

public class PackageDepartureNotFoundException extends RuntimeException {
    public PackageDepartureNotFoundException(UUID departureId) {
        super("Package departure not found: " + departureId);
    }
}
