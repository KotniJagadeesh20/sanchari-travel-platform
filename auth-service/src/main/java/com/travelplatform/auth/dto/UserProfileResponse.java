package com.travelplatform.auth.dto;

import com.travelplatform.auth.entity.UserAdmin;
import com.travelplatform.auth.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Date;
import java.util.UUID;

/**
 * Public user profile returned by GET /auth/users/{id}.
 * Used by domain services (bus-booking, ride-share, packages) to resolve
 * fresh user details (name, email) when the cached user_ref.email may be stale.
 *
 * NEVER includes password, refresh tokens, or any credential data.
 */
@Schema(description = "Public user profile — safe to return to domain services. Never contains credentials.")
public class UserProfileResponse {

    private UUID id;
    private String name;
    private String email;
    private String phone;
    private String gender;
    private int age;
    private Date dob;
    private Role role;

    public static UserProfileResponse from(UserAdmin user) {
        UserProfileResponse r = new UserProfileResponse();
        r.id = user.getId();
        r.name = user.getName();
        r.email = user.getEmail();
        r.phone = user.getPhone();
        r.gender = user.getGender();
        r.age = user.getAge();
        r.dob = user.getDob();
        r.role = user.getRole();
        return r;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getGender() { return gender; }
    public int getAge() { return age; }
    public Date getDob() { return dob; }
    public Role getRole() { return role; }
}
