package com.travelplatform.auth.controller;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

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
@Tag(name = "User Profile", description = "User profile lookup — for domain services needing fresh user data")
@SecurityRequirement(name = "bearerAuth")
public class UserProfileController {

    @Autowired
    private UserAdminRepository userAdminRepo;

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
}
