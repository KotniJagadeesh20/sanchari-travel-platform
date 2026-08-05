package com.travelplatform.hotel.dto;

import com.travelplatform.hotel.entity.Room;
import com.travelplatform.hotel.enums.RoomType;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class RoomResponse {

    private UUID id;
    private UUID hotelId;
    private String roomNumber;
    private RoomType roomType;
    private Integer capacity;
    private Double pricePerNight;
    private Integer totalRooms;
    private Integer availableRooms;
    private String description;
    private Boolean active;
    private List<String> imageUrls;
    private List<String> amenities;

    public static RoomResponse from(Room room) {
        RoomResponse r = new RoomResponse();
        r.id = room.getId();
        r.hotelId = room.getHotel() != null ? room.getHotel().getId() : null;
        r.roomNumber = room.getRoomNumber();
        r.roomType = room.getRoomType();
        r.capacity = room.getCapacity();
        r.pricePerNight = room.getPricePerNight();
        r.totalRooms = room.getTotalRooms();
        r.availableRooms = room.getAvailableRooms();
        r.description = room.getDescription();
        r.active = room.getActive();
        r.imageUrls = room.getImages().stream().map(i -> i.getImageUrl()).collect(Collectors.toList());
        r.amenities = room.getAmenities().stream().map(a -> a.getName()).collect(Collectors.toList());
        return r;
    }

    public UUID getId() { return id; }
    public UUID getHotelId() { return hotelId; }
    public String getRoomNumber() { return roomNumber; }
    public RoomType getRoomType() { return roomType; }
    public Integer getCapacity() { return capacity; }
    public Double getPricePerNight() { return pricePerNight; }
    public Integer getTotalRooms() { return totalRooms; }
    public Integer getAvailableRooms() { return availableRooms; }
    public String getDescription() { return description; }
    public Boolean getActive() { return active; }
    public List<String> getImageUrls() { return imageUrls; }
    public List<String> getAmenities() { return amenities; }
}
