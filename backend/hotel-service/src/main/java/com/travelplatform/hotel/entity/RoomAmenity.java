package com.travelplatform.hotel.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "room_amenity")
public class RoomAmenity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(nullable = false)
    private String name;

    public RoomAmenity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
