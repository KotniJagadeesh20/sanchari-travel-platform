package com.travelplatform.packages.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "One traveler's details for a package booking")
public class TravelerRequest {

    @NotBlank(message = "Traveler name is required")
    @Schema(example = "Asha Rao")
    private String name;

    @NotNull(message = "Traveler age is required")
    @Min(value = 1, message = "Age must be positive")
    @Schema(example = "29")
    private Integer age;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
}
