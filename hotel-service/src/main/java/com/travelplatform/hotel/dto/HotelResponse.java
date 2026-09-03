package com.travelplatform.hotel.dto;

import com.travelplatform.hotel.entity.Hotel;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class HotelResponse {

    private UUID id;
    private String name;
    private String description;
    private UUID destinationId;
    private String address;
    private String city;
    private String state;
    private String country;
    private Double latitude;
    private Double longitude;
    private Integer starRating;
    private String contactEmail;
    private String contactPhone;
    private LocalTime checkInTime;
    private LocalTime checkOutTime;
    private Boolean active;
    private Double averageRating;
    private Integer reviewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<String> imageUrls;
    private List<AmenityResponse> amenities;
    private List<RoomResponse> rooms;
    private UUID createdById;
    private String createdByName;
    private String createdByEmail;

    public static HotelResponse from(Hotel hotel) {
        HotelResponse r = new HotelResponse();
        r.id = hotel.getId();
        r.name = hotel.getName();
        r.description = hotel.getDescription();
        r.destinationId = hotel.getDestinationId();
        r.address = hotel.getAddress();
        r.city = hotel.getCity();
        r.state = hotel.getState();
        r.country = hotel.getCountry();
        r.latitude = hotel.getLatitude();
        r.longitude = hotel.getLongitude();
        r.starRating = hotel.getStarRating();
        r.contactEmail = hotel.getContactEmail();
        r.contactPhone = hotel.getContactPhone();
        r.checkInTime = hotel.getCheckInTime();
        r.checkOutTime = hotel.getCheckOutTime();
        r.active = hotel.getActive();
        r.averageRating = hotel.getAverageRating();
        r.reviewCount = hotel.getReviewCount();
        r.createdAt = hotel.getCreatedAt();
        r.updatedAt = hotel.getUpdatedAt();
        r.imageUrls = hotel.getImages().stream().map(i -> i.getImageUrl()).collect(Collectors.toList());
        r.amenities = hotel.getAmenities().stream()
                .map(a -> new AmenityResponse(a.getId(), a.getName(), a.getIcon()))
                .collect(Collectors.toList());
        if (hotel.getCreatedBy() != null) {
            r.createdById = hotel.getCreatedBy().getId();
            r.createdByName = hotel.getCreatedBy().getName();
            r.createdByEmail = hotel.getCreatedBy().getEmail();
        }
        return r;
    }

    /** Same as {@link #from}, but also embeds full room details — used for the hotel-details endpoint. */
    public static HotelResponse withRooms(Hotel hotel) {
        HotelResponse r = from(hotel);
        r.rooms = hotel.getRooms().stream().map(RoomResponse::from).collect(Collectors.toList());
        return r;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public UUID getDestinationId() { return destinationId; }
    public String getAddress() { return address; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getCountry() { return country; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public Integer getStarRating() { return starRating; }
    public String getContactEmail() { return contactEmail; }
    public String getContactPhone() { return contactPhone; }
    public LocalTime getCheckInTime() { return checkInTime; }
    public LocalTime getCheckOutTime() { return checkOutTime; }
    public Boolean getActive() { return active; }
    public Double getAverageRating() { return averageRating; }
    public Integer getReviewCount() { return reviewCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public List<String> getImageUrls() { return imageUrls; }
    public List<AmenityResponse> getAmenities() { return amenities; }
    public List<RoomResponse> getRooms() { return rooms; }
    public UUID getCreatedById() { return createdById; }
    public String getCreatedByName() { return createdByName; }
    public String getCreatedByEmail() { return createdByEmail; }

    public static class AmenityResponse {
        private UUID id;
        private String name;
        private String icon;

        public AmenityResponse(UUID id, String name, String icon) {
            this.id = id; this.name = name; this.icon = icon;
        }

        public UUID getId() { return id; }
        public String getName() { return name; }
        public String getIcon() { return icon; }
    }
}
