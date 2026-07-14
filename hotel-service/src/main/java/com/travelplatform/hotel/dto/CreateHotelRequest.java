package com.travelplatform.hotel.dto;

import jakarta.validation.constraints.*;

import java.time.LocalTime;
import java.util.UUID;

public class CreateHotelRequest {

    @NotBlank(message = "name is required")
    private String name;

    private String description;

    @NotNull(message = "destinationId is required")
    private UUID destinationId;

    @NotBlank(message = "address is required")
    private String address;

    @NotBlank(message = "city is required")
    private String city;

    private String state;

    @NotBlank(message = "country is required")
    private String country;

    private Double latitude;

    private Double longitude;

    @NotNull(message = "starRating is required")
    @Min(value = 1, message = "starRating must be between 1 and 5")
    @Max(value = 5, message = "starRating must be between 1 and 5")
    private Integer starRating;

    @Email(message = "contactEmail must be a valid email")
    private String contactEmail;

    private String contactPhone;

    private LocalTime checkInTime;

    private LocalTime checkOutTime;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public UUID getDestinationId() { return destinationId; }
    public void setDestinationId(UUID destinationId) { this.destinationId = destinationId; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Integer getStarRating() { return starRating; }
    public void setStarRating(Integer starRating) { this.starRating = starRating; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public LocalTime getCheckInTime() { return checkInTime; }
    public void setCheckInTime(LocalTime checkInTime) { this.checkInTime = checkInTime; }

    public LocalTime getCheckOutTime() { return checkOutTime; }
    public void setCheckOutTime(LocalTime checkOutTime) { this.checkOutTime = checkOutTime; }
}
