package com.travelplatform.packages.exception;

import java.util.UUID;

/** Thrown when attempting to book a package that is delisted (active = false). */
public class PackageNotBookableException extends RuntimeException {
    public PackageNotBookableException(UUID packageId) {
        super("Package " + packageId + " is not currently available for booking.");
    }
}
