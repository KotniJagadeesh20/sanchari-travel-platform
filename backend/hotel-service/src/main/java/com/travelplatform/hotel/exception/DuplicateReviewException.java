package com.travelplatform.hotel.exception;

import java.util.UUID;

public class DuplicateReviewException extends RuntimeException {
    public DuplicateReviewException(UUID hotelId) {
        super("You have already reviewed hotel: " + hotelId);
    }
}
