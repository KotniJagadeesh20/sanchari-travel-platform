package com.travelplatform.packages.destination.dto;

import com.travelplatform.packages.destination.entity.Attraction;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Attraction details returned to clients")
public class AttractionResponse {

    private UUID id;
    private String name;
    private String description;
    private String attractionType;

    public static AttractionResponse from(Attraction a) {
        AttractionResponse r = new AttractionResponse();
        r.id = a.getId();
        r.name = a.getName();
        r.description = a.getDescription();
        r.attractionType = a.getAttractionType();
        return r;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getAttractionType() { return attractionType; }
}
