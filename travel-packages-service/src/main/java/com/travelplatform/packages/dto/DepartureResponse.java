package com.travelplatform.packages.dto;

import com.travelplatform.packages.entity.PackageDeparture;

import java.time.LocalDate;
import java.util.UUID;

public class DepartureResponse {

    private UUID id;
    private LocalDate startDate;
    private LocalDate endDate; // derived: startDate + package's durationDays - 1
    private Integer maxPeople;
    private Integer availableSlots;
    private Boolean active;

    public static DepartureResponse from(PackageDeparture d) {
        DepartureResponse r = new DepartureResponse();
        r.id = d.getId();
        r.startDate = d.getStartDate();
        r.endDate = d.getStartDate().plusDays(Math.max(d.getTravelPackage().getDurationDays() - 1, 0));
        r.maxPeople = d.getMaxPeople();
        r.availableSlots = d.getAvailableSlots();
        r.active = d.getActive();
        return r;
    }

    public UUID getId() { return id; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public Integer getMaxPeople() { return maxPeople; }
    public Integer getAvailableSlots() { return availableSlots; }
    public Boolean getActive() { return active; }
}
