package com.travelplatform.packages.destination.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "One attraction/place within a destination")
public class AttractionRequest {

    @NotBlank(message = "Attraction name is required")
    @Schema(example = "Baga Beach")
    private String name;

    @Schema(example = "A lively beach known for water sports and nightlife.")
    private String description;

    @Schema(example = "Beach")
    private String attractionType;

    @Schema(example = "https://example.com/baga-beach.jpg")
    private String imageUrl;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAttractionType() { return attractionType; }
    public void setAttractionType(String attractionType) { this.attractionType = attractionType; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
