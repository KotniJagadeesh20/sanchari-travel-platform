package com.travelplatform.hotel.dto;

import jakarta.validation.constraints.NotBlank;

/** Reused for both "add hotel amenity" and "add room amenity" endpoints (icon is ignored for room amenities). */
public class AddAmenityRequest {

    @NotBlank(message = "name is required")
    private String name;

    private String icon;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
}
