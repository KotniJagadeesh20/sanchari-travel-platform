package com.travelplatform.packages.dto;

import com.travelplatform.packages.entity.PackageItinerary;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "One day of a package's itinerary, as returned to clients")
public class ItineraryDayResponse {

    private UUID id;
    private Integer dayNumber;
    private String plan;

    public static ItineraryDayResponse from(PackageItinerary day) {
        ItineraryDayResponse r = new ItineraryDayResponse();
        r.id = day.getId();
        r.dayNumber = day.getDayNumber();
        r.plan = day.getPlan();
        return r;
    }

    public UUID getId() { return id; }
    public Integer getDayNumber() { return dayNumber; }
    public String getPlan() { return plan; }
}
