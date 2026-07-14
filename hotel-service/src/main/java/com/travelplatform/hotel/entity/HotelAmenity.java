package com.travelplatform.hotel.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "hotel_amenity")
public class HotelAmenity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @Column(nullable = false)
    private String name;

    private String icon;

    public HotelAmenity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Hotel getHotel() { return hotel; }
    public void setHotel(Hotel hotel) { this.hotel = hotel; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
}
