package com.travelplatform.hotel.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

/** All fields optional (partial update). totalRooms changes reconcile availableRooms — see RoomServiceImpl. */
public class UpdateRoomRequest {

    private String roomNumber;

    @Min(value = 1, message = "capacity must be at least 1")
    private Integer capacity;

    @Positive(message = "pricePerNight must be positive")
    private Double pricePerNight;

    @Min(value = 0, message = "totalRooms cannot be negative")
    private Integer totalRooms;

    private String description;
    private Boolean active;

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public Double getPricePerNight() { return pricePerNight; }
    public void setPricePerNight(Double pricePerNight) { this.pricePerNight = pricePerNight; }

    public Integer getTotalRooms() { return totalRooms; }
    public void setTotalRooms(Integer totalRooms) { this.totalRooms = totalRooms; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
