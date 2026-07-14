package com.travelplatform.hotel.dto;

import com.travelplatform.hotel.enums.RoomType;
import jakarta.validation.constraints.*;

public class CreateRoomRequest {

    @NotBlank(message = "roomNumber is required")
    private String roomNumber;

    @NotNull(message = "roomType is required")
    private RoomType roomType;

    @NotNull(message = "capacity is required")
    @Min(value = 1, message = "capacity must be at least 1")
    private Integer capacity;

    @NotNull(message = "pricePerNight is required")
    @Positive(message = "pricePerNight must be positive")
    private Double pricePerNight;

    @NotNull(message = "totalRooms is required")
    @Min(value = 1, message = "totalRooms must be at least 1")
    private Integer totalRooms;

    private String description;

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public RoomType getRoomType() { return roomType; }
    public void setRoomType(RoomType roomType) { this.roomType = roomType; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public Double getPricePerNight() { return pricePerNight; }
    public void setPricePerNight(Double pricePerNight) { this.pricePerNight = pricePerNight; }

    public Integer getTotalRooms() { return totalRooms; }
    public void setTotalRooms(Integer totalRooms) { this.totalRooms = totalRooms; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
