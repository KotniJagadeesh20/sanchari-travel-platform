package com.travelplatform.packages.destination.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "One activity offered at a destination")
public class ActivityRequest {

    @NotBlank(message = "Activity name is required")
    @Schema(example = "Scuba Diving")
    private String name;

    @Schema(example = "Water Sports")
    private String category;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
