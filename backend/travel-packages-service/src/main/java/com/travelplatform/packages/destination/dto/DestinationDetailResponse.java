package com.travelplatform.packages.destination.dto;

import com.travelplatform.packages.destination.entity.Destination;
import com.travelplatform.packages.destination.enums.DestinationCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Schema(description = "Full destination details — used for the destination detail page")
public class DestinationDetailResponse {

    private UUID id;
    private String name;
    private String state;
    private String country;
    private String description;
    private String bestTimeToVisit;
    private Double averageBudget;
    private Integer recommendedDays;
    private DestinationCategory category;
    private Double manualRating;
    private Boolean active;
    private List<String> imageUrls;
    private List<AttractionResponse> attractions;
    private List<ActivityResponse> activities;

    public static DestinationDetailResponse from(Destination d) {
        DestinationDetailResponse r = new DestinationDetailResponse();
        r.id = d.getId();
        r.name = d.getName();
        r.state = d.getState();
        r.country = d.getCountry();
        r.description = d.getDescription();
        r.bestTimeToVisit = d.getBestTimeToVisit();
        r.averageBudget = d.getAverageBudget();
        r.recommendedDays = d.getRecommendedDays();
        r.category = d.getCategory();
        r.manualRating = d.getManualRating();
        r.active = d.getActive();
        r.imageUrls = d.getImageUrls();
        r.attractions = d.getAttractions().stream().map(AttractionResponse::from).collect(Collectors.toList());
        r.activities = d.getActivities().stream().map(ActivityResponse::from).collect(Collectors.toList());
        return r;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getState() { return state; }
    public String getCountry() { return country; }
    public String getDescription() { return description; }
    public String getBestTimeToVisit() { return bestTimeToVisit; }
    public Double getAverageBudget() { return averageBudget; }
    public Integer getRecommendedDays() { return recommendedDays; }
    public DestinationCategory getCategory() { return category; }
    public Double getManualRating() { return manualRating; }
    public Boolean getActive() { return active; }
    public List<String> getImageUrls() { return imageUrls; }
    public List<AttractionResponse> getAttractions() { return attractions; }
    public List<ActivityResponse> getActivities() { return activities; }
}
