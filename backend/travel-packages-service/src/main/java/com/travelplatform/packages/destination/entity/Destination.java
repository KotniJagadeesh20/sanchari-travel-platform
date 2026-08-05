package com.travelplatform.packages.destination.entity;

import com.travelplatform.packages.config.StringListConverter;
import com.travelplatform.packages.destination.enums.DestinationCategory;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "destination")
public class Destination {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String state;

    @Column(nullable = false)
    private String country;

    @Column(length = 5000)
    private String description;

    /**
     * Calendar months (1=January … 12=December) this destination is good to
     * visit. A proper @ElementCollection (its own join table) rather than a
     * free-text field like "Nov-Feb" specifically so it's queryable —
     * "destinations good to visit in March" is `3 MEMBER OF d.bestMonths`,
     * not string parsing. Empty/null means not specified.
     */
    @ElementCollection
    @CollectionTable(name = "destination_best_months", joinColumns = @JoinColumn(name = "destination_id"))
    @Column(name = "month")
    private Set<Integer> bestMonths = new HashSet<>();

    private Double averageBudget;

    private Integer recommendedDays;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DestinationCategory category;

    /**
     * Admin-set rating (0.0–5.0). NOT computed from user reviews — no review
     * system exists yet. This is an explicit manual value an admin can set
     * to surface quality destinations. Will be replaced by a computed aggregate
     * once a Review entity is built. Named "manualRating" rather than "rating"
     * to make clear it has no automatic writer.
     */
    private Double manualRating = 0.0;

    /** Soft-delist flag, same pattern as TravelPackage.active — preserves FK integrity for existing packages/bookings. */
    @Column(nullable = false)
    private Boolean active = true;

    /** Flat list, JSON-converted into one column — same StringListConverter used by TravelPackage's lists. */
    @Convert(converter = StringListConverter.class)
    @Column(length = 2000)
    private List<String> imageUrls = new ArrayList<>();

    @OneToMany(mappedBy = "destination", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Attraction> attractions = new ArrayList<>();

    @OneToMany(mappedBy = "destination", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Activity> activities = new ArrayList<>();

    public Destination() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Set<Integer> getBestMonths() { return bestMonths; }
    public void setBestMonths(Set<Integer> bestMonths) { this.bestMonths = bestMonths; }

    public Double getAverageBudget() { return averageBudget; }
    public void setAverageBudget(Double averageBudget) { this.averageBudget = averageBudget; }

    public Integer getRecommendedDays() { return recommendedDays; }
    public void setRecommendedDays(Integer recommendedDays) { this.recommendedDays = recommendedDays; }

    public DestinationCategory getCategory() { return category; }
    public void setCategory(DestinationCategory category) { this.category = category; }

    public Double getManualRating() { return manualRating; }
    public void setManualRating(Double manualRating) { this.manualRating = manualRating; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }

    public List<Attraction> getAttractions() { return attractions; }
    public void setAttractions(List<Attraction> attractions) { this.attractions = attractions; }

    public List<Activity> getActivities() { return activities; }
    public void setActivities(List<Activity> activities) { this.activities = activities; }
}
