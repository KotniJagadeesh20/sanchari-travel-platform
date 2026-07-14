package com.travelplatform.packages.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "One day of a package's itinerary")
public class ItineraryDayRequest {

    @NotNull(message = "Day number is required")
    @Min(value = 1, message = "Day number must be at least 1")
    @Schema(example = "1")
    private Integer dayNumber;

    @NotBlank(message = "Plan is required")
    @Schema(example = "Arrival and Hotel Check-in")
    private String plan;

    public Integer getDayNumber() { return dayNumber; }
    public void setDayNumber(Integer dayNumber) { this.dayNumber = dayNumber; }

    public String getPlan() { return plan; }
    public void setPlan(String plan) { this.plan = plan; }
}
