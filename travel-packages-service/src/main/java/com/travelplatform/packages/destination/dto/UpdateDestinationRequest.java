package com.travelplatform.packages.destination.dto;

import com.travelplatform.packages.destination.enums.DestinationCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

@Schema(description = "Request body for updating a destination. Only non-null fields are applied.")
public class UpdateDestinationRequest {

    private String name;
    private String state;
    private String country;
    private String description;
    private List<Integer> bestMonths;

    @PositiveOrZero(message = "Average budget cannot be negative")
    private Double averageBudget;

    @Min(value = 1, message = "Recommended days must be at least 1")
    private Integer recommendedDays;

    private DestinationCategory category;
    private Boolean active;

    @DecimalMin(value = "0.0", message = "manualRating cannot be negative")
    @DecimalMax(value = "5.0", message = "manualRating cannot exceed 5.0")
    @Schema(description = "Admin-set quality rating (0.0–5.0). Not computed from user reviews.", example = "4.5")
    private Double manualRating;

    private List<String> imageUrls;

    @Valid
    private List<AttractionRequest> attractions;

    @Valid
    private List<ActivityRequest> activities;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<Integer> getBestMonths() { return bestMonths; }
    public void setBestMonths(List<Integer> bestMonths) { this.bestMonths = bestMonths; }

    public Double getAverageBudget() { return averageBudget; }
    public void setAverageBudget(Double averageBudget) { this.averageBudget = averageBudget; }

    public Integer getRecommendedDays() { return recommendedDays; }
    public void setRecommendedDays(Integer recommendedDays) { this.recommendedDays = recommendedDays; }

    public DestinationCategory getCategory() { return category; }
    public void setCategory(DestinationCategory category) { this.category = category; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Double getManualRating() { return manualRating; }
    public void setManualRating(Double manualRating) { this.manualRating = manualRating; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }

    public List<AttractionRequest> getAttractions() { return attractions; }
    public void setAttractions(List<AttractionRequest> attractions) { this.attractions = attractions; }

    public List<ActivityRequest> getActivities() { return activities; }
    public void setActivities(List<ActivityRequest> activities) { this.activities = activities; }
}
