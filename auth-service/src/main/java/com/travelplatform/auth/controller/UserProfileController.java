package com.travelplatform.auth.controller;

import com.travelplatform.auth.dto.UpdateProfileRequest;
import com.travelplatform.auth.dto.UserProfileResponse;
import com.travelplatform.auth.entity.UserAdmin;
import com.travelplatform.auth.repository.UserAdminRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Internal user profile lookup — used by domain services (bus-booking,
 * ride-share, packages) when they need fresh user data (name, email)
 * rather than the potentially-stale cached copy in their own user_ref table.
 *
 * Requires a valid JWT — the gateway forwards X-Authenticated-* headers
 * here just like any other protected endpoint.
 */
@RestController
@RequestMapping("/auth/users")
@Validated
@Tag(name = "User Profile", description = "User profile lookup and self-service update")
@SecurityRequirement(name = "bearerAuth")
public class UserProfileController {

    @Autowired
    private UserAdminRepository userAdminRepo;

    @Operation(
        summary = "List all users (admin only)",
        description = "For the Admin Dashboard's user management view. Never returns password or credentials " +
                "(same UserProfileResponse used everywhere else here). No suspend/delete capability exists yet " +
                "— UserAdmin has no enabled/status field, so there's nothing to toggle; adding one is a " +
                "separate, more careful change since it touches the login/auth path for every user."
    )
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/admin")
    public ResponseEntity<List<UserProfileResponse>> getAllUsers() {
        List<UserProfileResponse> users = userAdminRepo.findAll()
                .stream().map(UserProfileResponse::from).collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @Operation(
        summary = "Get user profile by UUID",
        description = "Returns non-sensitive user details (name, email, role). " +
                "Called by domain services when their cached user_ref.email may be stale " +
                "after a user updates their email in auth-service. Never returns password or credentials."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User profile returned"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserById(
            @Parameter(description = "User UUID") @PathVariable UUID userId) {

        UserAdmin user = userAdminRepo.findById(userId).orElse(null);

        if (user == null) {
            return ResponseEntity.status(404).body(
                Map.of("success", false, "message", "User not found: " + userId));
        }

        return ResponseEntity.ok(UserProfileResponse.from(user));
    }

    @Operation(
        summary = "Get user profile by email",
        description = "Alternate lookup by email address. Useful when a domain service only has the " +
                "X-Authenticated-Email header and needs the full profile including the UUID."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User profile returned"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/by-email/{email}")
    public ResponseEntity<?> getUserByEmail(
            @Parameter(description = "User email address") @PathVariable String email) {

        UserAdmin user = userAdminRepo.findByEmail(email);

        if (user == null) {
            return ResponseEntity.status(404).body(
                Map.of("success", false, "message", "User not found: " + email));
        }

        return ResponseEntity.ok(UserProfileResponse.from(user));
    }

    @Operation(
        summary = "Update my own profile",
        description = "Partial update of the caller's own profile — name/phone/gender/age/dob only. " +
                "No email or password here (see UpdateProfileRequest's Javadoc for why). Only non-null " +
                "fields in the request body are applied; omitted fields are left unchanged. " +
                "Always updates the CALLER's own record (from the gateway-forwarded " +
                "X-Authenticated-User-Id header) — there's no user-id path parameter, so there's no " +
                "ownership check to get wrong."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile updated"),
        @ApiResponse(responseCode = "404", description = "Authenticated user not found")
    })
    @PutMapping("/me")
    public ResponseEntity<?> updateMyProfile(
            @Validated @RequestBody UpdateProfileRequest request,
            @Parameter(hidden = true) @RequestHeader("X-Authenticated-User-Id") String userIdStr) {

        UserAdmin user = userAdminRepo.findById(UUID.fromString(userIdStr)).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(
                Map.of("success", false, "message", "User not found: " + userIdStr));
        }

        if (request.getName() != null) user.setName(request.getName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getGender() != null) user.setGender(request.getGender());
        if (request.getAge() != null) user.setAge(request.getAge());
        if (request.getDob() != null) user.setDob(request.getDob());

        userAdminRepo.save(user);
        return ResponseEntity.ok(UserProfileResponse.from(user));
    }
}
