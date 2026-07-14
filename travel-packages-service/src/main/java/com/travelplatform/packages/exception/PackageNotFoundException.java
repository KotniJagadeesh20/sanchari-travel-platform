package com.travelplatform.packages.exception;

import java.util.UUID;

public class PackageNotFoundException extends RuntimeException {
    public PackageNotFoundException(UUID packageId) {
        super("Travel package not found: " + packageId);
    }
}
