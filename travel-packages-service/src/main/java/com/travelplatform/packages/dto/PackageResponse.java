package com.travelplatform.packages.dto;

import com.travelplatform.packages.entity.TravelPackage;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Schema(description = "Full package details returned to clients")
public class PackageResponse {

    private UUID id;
    private String title;
    private String description;
    private UUID destinationId;
    private Integer durationDays;
    private Integer durationNights;
    private Double price;
    private Integer maxPeople;
    private String thumbnailImage;
    private Boolean active;
    private List<String> inclusions;
    private List<String> exclusions;
    private List<String> placesCovered;
    private List<String> activities;
    private List<String> imageUrls;
    private List<ItineraryDayResponse> itinerary;
    /** All departure batches, active or not, past or future — caller filters as needed (e.g. customer UI shows only active + upcoming). */
    private List<DepartureResponse> departures;
    private UUID createdById;
    private String createdByName;
    private String createdByEmail;

    public static PackageResponse from(TravelPackage pkg) {
        PackageResponse r = new PackageResponse();
        r.id = pkg.getId();
        r.title = pkg.getTitle();
        r.description = pkg.getDescription();
        r.destinationId = pkg.getDestinationId();
        r.durationDays = pkg.getDurationDays();
        r.durationNights = pkg.getDurationNights();
        r.price = pkg.getPrice();
        r.maxPeople = pkg.getMaxPeople();
        r.thumbnailImage = pkg.getThumbnailImage();
        r.active = pkg.getActive();
        r.inclusions = pkg.getInclusions();
        r.exclusions = pkg.getExclusions();
        r.placesCovered = pkg.getPlacesCovered();
        r.activities = pkg.getActivities();
        r.imageUrls = pkg.getImageUrls();
        r.itinerary = pkg.getItinerary().stream()
                .map(ItineraryDayResponse::from)
                .collect(Collectors.toList());
        r.departures = pkg.getDepartures().stream()
                .map(DepartureResponse::from)
                .collect(Collectors.toList());
        if (pkg.getCreatedBy() != null) {
            r.createdById = pkg.getCreatedBy().getId();
            r.createdByName = pkg.getCreatedBy().getName();
            r.createdByEmail = pkg.getCreatedBy().getEmail();
        }
        return r;
    }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public UUID getDestinationId() { return destinationId; }
    public Integer getDurationDays() { return durationDays; }
    public Integer getDurationNights() { return durationNights; }
    public Double getPrice() { return price; }
    public Integer getMaxPeople() { return maxPeople; }
    public String getThumbnailImage() { return thumbnailImage; }
    public Boolean getActive() { return active; }
    public List<String> getInclusions() { return inclusions; }
    public List<String> getExclusions() { return exclusions; }
    public List<String> getPlacesCovered() { return placesCovered; }
    public List<String> getActivities() { return activities; }
    public List<String> getImageUrls() { return imageUrls; }
    public List<ItineraryDayResponse> getItinerary() { return itinerary; }
    public List<DepartureResponse> getDepartures() { return departures; }
    public UUID getCreatedById() { return createdById; }
    public String getCreatedByName() { return createdByName; }
    public String getCreatedByEmail() { return createdByEmail; }
}
