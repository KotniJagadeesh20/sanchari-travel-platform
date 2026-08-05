package com.travelplatform.rideshare.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Lightweight user reference, mirroring the pattern used in bus-booking-service.
 * ride-share-service does not own user accounts — that's auth-service's job.
 * We persist the UUID as the authoritative FK reference. Email is cached as a
 * display convenience but may become stale if the user changes it in auth-service;
 * call GET /auth/users/{id} for fresh user data when accuracy is critical.
 *
 * Any user can be a driver on one ride and a passenger on another — there is
 * no ROLE_DRIVER. "Driver" just means "the UserRef referenced by Ride.driver".
 */
@Entity
@Table(name = "user_ref")
public class UserRef {

    @Id
    private UUID id;

    /** Cached display hint — may be stale if user changed email. Not authoritative. */
    @Column(nullable = true)
    private String email;

    /** Cached display hint from auth-service's UserAdmin.name — may go stale on rename. */
    @Column(nullable = true)
    private String name;

    public UserRef() {}

    public UserRef(UUID id, String email, String name) {
        this.id = id;
        this.email = email;
        this.name = name;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
