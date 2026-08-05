package com.travelplatform.packages.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "Request body for booking a travel package departure")
public class BookPackageRequest {

    @NotEmpty(message = "At least one traveler is required")
    @Valid
    @Schema(description = "Names and ages of everyone traveling on this booking")
    private List<TravelerRequest> travelers;

    public List<TravelerRequest> getTravelers() { return travelers; }
    public void setTravelers(List<TravelerRequest> travelers) { this.travelers = travelers; }
}
