package com.travelplatform.packages.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * One bookable departure batch for a TravelPackage. A package template (e.g.
 * "Goa Beach Getaway") can have several of these — different start dates,
 * each tracking its own slot availability independently, mirroring how
 * hotel-service splits Room out from Hotel.
 */
@Entity
@Table(name = "package_departure")
public class PackageDeparture {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "travel_package_id", referencedColumnName = "id", nullable = false)
    private TravelPackage travelPackage;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private Integer maxPeople;

    @Column(nullable = false)
    private Integer availableSlots;

    /** Lets a partner cancel one specific batch without affecting the package template or its other departures. */
    @Column(nullable = false)
    private Boolean active = true;

    public PackageDeparture() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public TravelPackage getTravelPackage() { return travelPackage; }
    public void setTravelPackage(TravelPackage travelPackage) { this.travelPackage = travelPackage; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public Integer getMaxPeople() { return maxPeople; }
    public void setMaxPeople(Integer maxPeople) { this.maxPeople = maxPeople; }

    public Integer getAvailableSlots() { return availableSlots; }
    public void setAvailableSlots(Integer availableSlots) { this.availableSlots = availableSlots; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
