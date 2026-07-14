package com.travelplatform.hotel.exception;

import java.util.UUID;

public class RoomNotAvailableException extends RuntimeException {
    public RoomNotAvailableException(UUID roomId) {
        super("Room " + roomId + " has no available inventory for the requested dates");
    }
}
