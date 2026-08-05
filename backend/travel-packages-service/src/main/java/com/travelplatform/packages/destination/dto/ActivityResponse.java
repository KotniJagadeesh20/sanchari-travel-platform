package com.travelplatform.packages.destination.dto;

import com.travelplatform.packages.destination.entity.Activity;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Activity details returned to clients")
public class ActivityResponse {

    private UUID id;
    private String name;
    private String category;
    private String imageUrl;

    public static ActivityResponse from(Activity a) {
        ActivityResponse r = new ActivityResponse();
        r.id = a.getId();
        r.name = a.getName();
        r.category = a.getCategory();
        r.imageUrl = a.getImageUrl();
        return r;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getImageUrl() { return imageUrl; }
}
