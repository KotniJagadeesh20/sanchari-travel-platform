package com.travelplatform.packages.entity;

import jakarta.persistence.*;

import java.util.UUID;

/** One named traveler on a PackageBooking — a partner needs real names/ages to arrange hotels/transport for the group. */
@Entity
@Table(name = "package_traveler")
public class PackageTraveler {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "booking_id", referencedColumnName = "id", nullable = false)
    private PackageBooking booking;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer age;

    public PackageTraveler() {}

    public PackageTraveler(String name, Integer age) {
        this.name = name;
        this.age = age;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public PackageBooking getBooking() { return booking; }
    public void setBooking(PackageBooking booking) { this.booking = booking; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
}
