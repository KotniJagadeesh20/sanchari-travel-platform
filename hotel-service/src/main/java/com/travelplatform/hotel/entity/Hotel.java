package com.travelplatform.hotel.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "hotel")
public class Hotel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(length = 5000)
    private String description;

    /**
     * FK to Destination, owned by travel-packages-service. Stored as a plain UUID —
     * never as a JPA relationship — since Hotel Service does not share a database
     * with, or depend on, the Travel Service. See ARCHITECTURE notes in the
     * project's Hotel Service design doc for the bounded-context reasoning.
     */
    @Column(nullable = false)
    private UUID destinationId;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String city;

    private String state;

    @Column(nullable = false)
    private String country;

    private Double latitude;

    private Double longitude;

    @Column(nullable = false)
    private Integer starRating;

    private String contactEmail;

    private String contactPhone;

    private LocalTime checkInTime;

    private LocalTime checkOutTime;

    /** Soft-delist flag — admin can hide a hotel without deleting booking/review history. */
    @Column(nullable = false)
    private Boolean active = true;

    /** Denormalized from HotelReview on every new review — avoids an AVG() query on every read. */
    @Column(nullable = false)
    private Double averageRating = 0.0;

    @Column(nullable = false)
    private Integer reviewCount = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "hotel", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    private List<HotelImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "hotel", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<HotelAmenity> amenities = new ArrayList<>();

    @OneToMany(mappedBy = "hotel", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Room> rooms = new ArrayList<>();

    /**
     * Who created this hotel, synced from JWT claims forwarded by the gateway.
     * Nullable: hotels created before this field existed have no value here
     * (ROLE_ADMIN was used to create hotels then too, but the identity wasn't
     * tracked). Not enforced as an edit/delist/manage restriction yet — any
     * ROLE_ADMIN can still manage any hotel, same as before. This just enables
     * showing "who's running this property" and scoping a "my hotels" list,
     * ahead of the full ROLE_PARTNER ownership model described in the V2
     * roadmap (mirrors TravelPackage.createdBy exactly).
     */
    @ManyToOne
    @JoinColumn(name = "created_by_id", referencedColumnName = "id", nullable = true)
    private UserRef createdBy;

    public UserRef getCreatedBy() { return createdBy; }
    public void setCreatedBy(UserRef createdBy) { this.createdBy = createdBy; }

    public Hotel() {}

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public UUID getDestinationId() { return destinationId; }
    public void setDestinationId(UUID destinationId) { this.destinationId = destinationId; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Integer getStarRating() { return starRating; }
    public void setStarRating(Integer starRating) { this.starRating = starRating; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public LocalTime getCheckInTime() { return checkInTime; }
    public void setCheckInTime(LocalTime checkInTime) { this.checkInTime = checkInTime; }

    public LocalTime getCheckOutTime() { return checkOutTime; }
    public void setCheckOutTime(LocalTime checkOutTime) { this.checkOutTime = checkOutTime; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Double getAverageRating() { return averageRating; }
    public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }

    public Integer getReviewCount() { return reviewCount; }
    public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<HotelImage> getImages() { return images; }
    public void setImages(List<HotelImage> images) { this.images = images; }

    public List<HotelAmenity> getAmenities() { return amenities; }
    public void setAmenities(List<HotelAmenity> amenities) { this.amenities = amenities; }

    public List<Room> getRooms() { return rooms; }
    public void setRooms(List<Room> rooms) { this.rooms = rooms; }
}
