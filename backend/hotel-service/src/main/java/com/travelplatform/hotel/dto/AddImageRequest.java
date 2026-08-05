package com.travelplatform.hotel.dto;

import jakarta.validation.constraints.NotBlank;

/** Reused for both "add hotel image" and "add room image" endpoints. */
public class AddImageRequest {

    @NotBlank(message = "imageUrl is required")
    private String imageUrl;

    private Integer displayOrder = 0;

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
}
