package com.travelplatform.packages.destination.dto;

import com.travelplatform.packages.destination.entity.Destination;
import com.travelplatform.packages.destination.enums.DestinationCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/**
 * Lightweight destination shape for list/browse/search views — avoids
 * shipping every attraction/activity for what's typically a results grid.
 * Matches the doc's DestinationResponse record shape.
 */
@Schema(description = "Summary destination details for browse/search result lists")
public class DestinationSummaryResponse {

    private UUID id;
    private String name;
    private String state;
    private String country;
    private String description;
    private Double averageBudget;
    private Double manualRating;
    private DestinationCategory category;
    private String thumbnailImage;

    public static DestinationSummaryResponse from(Destination d) {
        DestinationSummaryResponse r = new DestinationSummaryResponse();
        r.id = d.getId();
        r.name = d.getName();
        r.state = d.getState();
        r.country = d.getCountry();
        r.description = d.getDescription();
        r.averageBudget = d.getAverageBudget();
        r.manualRating = d.getManualRating();
        r.category = d.getCategory();
        r.thumbnailImage = d.getImageUrls().isEmpty() ? null : d.getImageUrls().get(0);
        return r;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getState() { return state; }
    public String getCountry() { return country; }
    public String getDescription() { return description; }
    public Double getAverageBudget() { return averageBudget; }
    public Double getManualRating() { return manualRating; }
    public DestinationCategory getCategory() { return category; }
    public String getThumbnailImage() { return thumbnailImage; }
}
