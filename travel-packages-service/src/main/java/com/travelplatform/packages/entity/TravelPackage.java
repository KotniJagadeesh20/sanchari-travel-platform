package com.travelplatform.packages.entity;

import com.travelplatform.packages.config.StringListConverter;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "travel_package")
public class TravelPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(length = 5000)
    private String description;

    /**
     * FK to Destination (same database — travel-packages-service hosts both modules).
     * Required: every package must be linked to a Destination so it can be
     * browsed by category/budget/keyword via the destination module.
     * The destination's name for display comes from DestinationService.getById(destinationId).
     */
    @Column(nullable = false)
    private UUID destinationId;

    @Column(nullable = false)
    private Integer durationDays;

    @Column(nullable = false)
    private Integer durationNights;

    @Column(nullable = false)
    private Double price;

    /**
     * Default capacity used when a partner adds a new PackageDeparture batch
     * without specifying one. Actual per-batch capacity/availability now
     * lives on PackageDeparture (see departures below) — a package can run
     * on several dates at once, each tracked independently, mirroring how
     * hotel-service splits Room out from Hotel.
     */
    @Column(nullable = false)
    private Integer maxPeople;

    private String thumbnailImage;

    /**
     * Whether this package is published/visible to customers. New packages
     * default to false (draft) — a partner reviews and explicitly publishes
     * (PUT .../admin/{id} with active=true) before it's browsable. The same
     * flag also serves as "delisted" for a previously-published package that's
     * pulled later; the two states aren't distinguished at the data level.
     */
    @Column(nullable = false)
    private Boolean active = false;

    @Convert(converter = StringListConverter.class)
    @Column(length = 2000)
    private List<String> inclusions = new ArrayList<>();

    @Convert(converter = StringListConverter.class)
    @Column(length = 2000)
    private List<String> exclusions = new ArrayList<>();

    @Convert(converter = StringListConverter.class)
    @Column(length = 2000)
    private List<String> placesCovered = new ArrayList<>();

    @Convert(converter = StringListConverter.class)
    @Column(length = 2000)
    private List<String> activities = new ArrayList<>();

    @Convert(converter = StringListConverter.class)
    @Column(length = 2000)
    private List<String> imageUrls = new ArrayList<>();

    @OneToMany(mappedBy = "travelPackage", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("dayNumber ASC")
    private List<PackageItinerary> itinerary = new ArrayList<>();

    /** Bookable departure batches — see PackageDeparture for why this is a separate entity. */
    @OneToMany(mappedBy = "travelPackage", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("startDate ASC")
    private List<PackageDeparture> departures = new ArrayList<>();

    /**
     * Who created/manages this package. Nullable — packages created before
     * this field existed have no value here (ROLE_ADMIN was used to create
     * packages then too, but the identity wasn't tracked). Not enforced as an
     * edit/delist restriction yet: any ROLE_ADMIN can still manage any
     * package, same as before. This just enables showing "who's delivering
     * this" to customers and scoping a "my packages" list, ahead of the full
     * ROLE_PARTNER ownership model described in the V2 roadmap.
     */
    @ManyToOne
    @JoinColumn(name = "created_by_id", referencedColumnName = "id", nullable = true)
    private UserRef createdBy;

    public UserRef getCreatedBy() { return createdBy; }
    public void setCreatedBy(UserRef createdBy) { this.createdBy = createdBy; }

    public TravelPackage() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public UUID getDestinationId() { return destinationId; }
    public void setDestinationId(UUID destinationId) { this.destinationId = destinationId; }

    public Integer getDurationDays() { return durationDays; }
    public void setDurationDays(Integer durationDays) { this.durationDays = durationDays; }

    public Integer getDurationNights() { return durationNights; }
    public void setDurationNights(Integer durationNights) { this.durationNights = durationNights; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public Integer getMaxPeople() { return maxPeople; }
    public void setMaxPeople(Integer maxPeople) { this.maxPeople = maxPeople; }

    public String getThumbnailImage() { return thumbnailImage; }
    public void setThumbnailImage(String thumbnailImage) { this.thumbnailImage = thumbnailImage; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public List<String> getInclusions() { return inclusions; }
    public void setInclusions(List<String> inclusions) { this.inclusions = inclusions; }

    public List<String> getExclusions() { return exclusions; }
    public void setExclusions(List<String> exclusions) { this.exclusions = exclusions; }

    public List<String> getPlacesCovered() { return placesCovered; }
    public void setPlacesCovered(List<String> placesCovered) { this.placesCovered = placesCovered; }

    public List<String> getActivities() { return activities; }
    public void setActivities(List<String> activities) { this.activities = activities; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }

    public List<PackageItinerary> getItinerary() { return itinerary; }
    public void setItinerary(List<PackageItinerary> itinerary) { this.itinerary = itinerary; }

    public List<PackageDeparture> getDepartures() { return departures; }
    public void setDepartures(List<PackageDeparture> departures) { this.departures = departures; }
}
