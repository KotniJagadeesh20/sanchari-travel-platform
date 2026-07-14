package com.travelplatform.packages.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Schema(description = "Request body for adding a bookable departure batch to a package")
public class DepartureRequest {

    @NotNull(message = "startDate is required")
    @Future(message = "startDate must be in the future")
    @Schema(example = "2026-08-15")
    private LocalDate startDate;

    @Min(value = 1, message = "maxPeople must be at least 1")
    @Schema(description = "Capacity for this batch — defaults to the package's maxPeople if omitted", example = "20")
    private Integer maxPeople;

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public Integer getMaxPeople() { return maxPeople; }
    public void setMaxPeople(Integer maxPeople) { this.maxPeople = maxPeople; }
}
