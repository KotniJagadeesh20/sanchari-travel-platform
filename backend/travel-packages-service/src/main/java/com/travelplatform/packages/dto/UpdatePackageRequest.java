package com.travelplatform.packages.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

import java.util.List;

@Schema(description = "Request body for updating a package. Only non-null fields are applied. " +
        "maxPeople here is just the default used for future departure batches — it no longer shifts " +
        "availableSlots directly, since availability now lives per-departure (see PackageDeparture).")
public class UpdatePackageRequest {

    private String title;
    private String description;
    private UUID destinationId;

    @Min(value = 1, message = "Duration must be at least 1 day")
    private Integer durationDays;

    @Min(value = 0, message = "Nights cannot be negative")
    private Integer durationNights;

    @Positive(message = "Price must be positive")
    private Double price;

    @Min(value = 1, message = "Must allow at least 1 traveler")
    private Integer maxPeople;

    private String thumbnailImage;
    private Boolean active;

    private List<String> inclusions;
    private List<String> exclusions;
    private List<String> placesCovered;
    private List<String> activities;
    private List<String> imageUrls;

    @Valid
    private List<ItineraryDayRequest> itinerary;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public UUID getDestinationId() { return destinationId; }
    public void setDestinationId(UUID destinationId) { this.destinationId = destinationId; }

    public Integer getDurationDays() { return durationDays; }
    public void setDurationDays(Integer durationDays) { this.durationDays = durationDays; }

    public Integer getDurationNights() { return durationNights; }
    public void setDurationNights(Integer durationNights) { this.durationNights = durationNights; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public Integer getMaxPeople() { return maxPeople; }
    public void setMaxPeople(Integer maxPeople) { this.maxPeople = maxPeople; }

    public String getThumbnailImage() { return thumbnailImage; }
    public void setThumbnailImage(String thumbnailImage) { this.thumbnailImage = thumbnailImage; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public List<String> getInclusions() { return inclusions; }
    public void setInclusions(List<String> inclusions) { this.inclusions = inclusions; }

    public List<String> getExclusions() { return exclusions; }
    public void setExclusions(List<String> exclusions) { this.exclusions = exclusions; }

    public List<String> getPlacesCovered() { return placesCovered; }
    public void setPlacesCovered(List<String> placesCovered) { this.placesCovered = placesCovered; }

    public List<String> getActivities() { return activities; }
    public void setActivities(List<String> activities) { this.activities = activities; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }

    public List<ItineraryDayRequest> getItinerary() { return itinerary; }
    public void setItinerary(List<ItineraryDayRequest> itinerary) { this.itinerary = itinerary; }
}
