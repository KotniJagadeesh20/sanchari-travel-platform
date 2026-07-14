package com.travelplatform.busbooking.entity;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * User reference entity in bus-booking-service.
 * This service does NOT manage user accounts — that belongs to auth-service.
 * We store only the UUID as the authoritative reference; email is cached for
 * convenience in booking history displays but is NOT kept in sync — if a user
 * changes their email in auth-service, call GET /auth/users/{id} for fresh data.
 * email is nullable and has no uniqueness constraint here: it's a display hint,
 * not an identity source.
 */
@Entity
@Table(name = "user_ref")
public class UserAdmin {

    @Id
    private UUID id;

    /** Cached display hint — may be stale if user changed email. Not authoritative. */
    @Column(nullable = true)
    private String email;

    public UserAdmin() {}

    public UserAdmin(UUID id, String email) {
        this.id = id;
        this.email = email;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
