package com.travelplatform.packages.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "package_itinerary")
public class PackageItinerary {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private Integer dayNumber;

    @Column(length = 3000, nullable = false)
    private String plan;

    @ManyToOne
    @JoinColumn(name = "travel_package_id", referencedColumnName = "id", nullable = false)
    private TravelPackage travelPackage;

    public PackageItinerary() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Integer getDayNumber() { return dayNumber; }
    public void setDayNumber(Integer dayNumber) { this.dayNumber = dayNumber; }

    public String getPlan() { return plan; }
    public void setPlan(String plan) { this.plan = plan; }

    public TravelPackage getTravelPackage() { return travelPackage; }
    public void setTravelPackage(TravelPackage travelPackage) { this.travelPackage = travelPackage; }
}
