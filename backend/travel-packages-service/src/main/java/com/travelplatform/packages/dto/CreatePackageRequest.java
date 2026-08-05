package com.travelplatform.packages.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

@Schema(description = "Request body for creating a travel package (admin only)")
public class CreatePackageRequest {

    @NotBlank(message = "Title is required")
    @Schema(example = "Goa Beach Getaway")
    private String title;

    @Schema(example = "A relaxing 4-day trip covering North and South Goa beaches.")
    private String description;

    @NotNull(message = "destinationId is required — create a Destination first via POST /destinations/admin")
    @Schema(description = "UUID of the Destination this package belongs to", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID destinationId;

    @NotNull(message = "Duration in days is required")
    @Min(value = 1, message = "Duration must be at least 1 day")
    @Schema(example = "4")
    private Integer durationDays;

    @NotNull(message = "Duration in nights is required")
    @Min(value = 0, message = "Nights cannot be negative")
    @Schema(example = "3")
    private Integer durationNights;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    @Schema(example = "15999.0")
    private Double price;

    @NotNull(message = "Max people is required")
    @Min(value = 1, message = "Must allow at least 1 traveler")
    @Schema(example = "20")
    private Integer maxPeople;

    @Schema(example = "https://cdn.example.com/goa-thumb.jpg")
    private String thumbnailImage;

    @Schema(description = "What's included in the price", example = "[\"Hotel Stay\", \"Breakfast\", \"Airport Pickup\"]")
    private List<String> inclusions;

    @Schema(description = "What's NOT included", example = "[\"Flights\", \"Personal Expenses\"]")
    private List<String> exclusions;

    @Schema(description = "Places visited", example = "[\"Baga Beach\", \"Dudhsagar Falls\"]")
    private List<String> placesCovered;

    @Schema(description = "Activities offered", example = "[\"Scuba Diving\", \"Parasailing\"]")
    private List<String> activities;

    @Schema(description = "Gallery image URLs")
    private List<String> imageUrls;

    @Valid
    @Schema(description = "Day-wise itinerary")
    private List<ItineraryDayRequest> itinerary;

    @Valid
    @Schema(description = "Optional initial bookable departure batch(es) — more can be added later via POST /packages/admin/{id}/departures")
    private List<DepartureRequest> departures;

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

    public List<DepartureRequest> getDepartures() { return departures; }
    public void setDepartures(List<DepartureRequest> departures) { this.departures = departures; }
}
