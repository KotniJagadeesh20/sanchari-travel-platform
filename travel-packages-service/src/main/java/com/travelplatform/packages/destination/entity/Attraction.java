package com.travelplatform.packages.destination.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "attraction")
public class Attraction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(length = 3000)
    private String description;

    /** Free-text type label (e.g. "Beach", "Waterfall", "Temple") — not an enum since attraction types vary too widely to enumerate up front. */
    private String attractionType;

    @ManyToOne
    @JoinColumn(name = "destination_id", referencedColumnName = "id", nullable = false)
    private Destination destination;

    public Attraction() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAttractionType() { return attractionType; }
    public void setAttractionType(String attractionType) { this.attractionType = attractionType; }

    public Destination getDestination() { return destination; }
    public void setDestination(Destination destination) { this.destination = destination; }
}
