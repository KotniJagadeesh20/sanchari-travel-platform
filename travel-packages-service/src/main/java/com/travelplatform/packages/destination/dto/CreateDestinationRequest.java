package com.travelplatform.packages.destination.dto;

import com.travelplatform.packages.destination.enums.DestinationCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

@Schema(description = "Request body for creating a destination (admin only)")
public class CreateDestinationRequest {

    @NotBlank(message = "Name is required")
    @Schema(example = "Goa")
    private String name;

    @Schema(example = "Goa")
    private String state;

    @NotBlank(message = "Country is required")
    @Schema(example = "India")
    private String country;

    @Schema(example = "Popular beach destination on India's west coast.")
    private String description;

    @Schema(example = "Nov-Feb")
    private String bestTimeToVisit;

    @PositiveOrZero(message = "Average budget cannot be negative")
    @Schema(example = "12000.0")
    private Double averageBudget;

    @Min(value = 1, message = "Recommended days must be at least 1")
    @Schema(example = "4")
    private Integer recommendedDays;

    @NotNull(message = "Category is required")
    @Schema(example = "BEACH")
    private DestinationCategory category;

    @Schema(description = "Admin-set quality rating 0.0–5.0. Optional at creation — defaults to 0.0.")
    private Double manualRating;

    @Schema(description = "Gallery image URLs")
    private List<String> imageUrls;

    @Valid
    @Schema(description = "Notable places within this destination")
    private List<AttractionRequest> attractions;

    @Valid
    @Schema(description = "Activities offered at this destination")
    private List<ActivityRequest> activities;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getBestTimeToVisit() { return bestTimeToVisit; }
    public void setBestTimeToVisit(String bestTimeToVisit) { this.bestTimeToVisit = bestTimeToVisit; }

    public Double getAverageBudget() { return averageBudget; }
    public void setAverageBudget(Double averageBudget) { this.averageBudget = averageBudget; }

    public Integer getRecommendedDays() { return recommendedDays; }
    public void setRecommendedDays(Integer recommendedDays) { this.recommendedDays = recommendedDays; }

    public DestinationCategory getCategory() { return category; }
    public void setCategory(DestinationCategory category) { this.category = category; }

    public Double getManualRating() { return manualRating; }
    public void setManualRating(Double manualRating) { this.manualRating = manualRating; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }

    public List<AttractionRequest> getAttractions() { return attractions; }
    public void setAttractions(List<AttractionRequest> attractions) { this.attractions = attractions; }

    public List<ActivityRequest> getActivities() { return activities; }
    public void setActivities(List<ActivityRequest> activities) { this.activities = activities; }
}
