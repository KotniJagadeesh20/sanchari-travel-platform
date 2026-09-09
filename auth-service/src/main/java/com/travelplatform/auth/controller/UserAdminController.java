package com.travelplatform.auth.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.travelplatform.auth.dto.UserAdminResponse;
import com.travelplatform.auth.dto.LoginRequest;
import com.travelplatform.auth.dto.RefreshTokenRequest;
import com.travelplatform.auth.dto.RegisterRequest;
import com.travelplatform.auth.dto.TokenRefreshResponse;
import com.travelplatform.auth.enums.Role;
import com.travelplatform.auth.repository.UserAdminRepository;
import com.travelplatform.auth.service.AuthService;
import com.travelplatform.auth.service.AuthService.AuthResult;

/**
 * Thin HTTP adapter for authentication.
 * All business logic lives in {@link AuthService} / {@link com.travelplatform.auth.service.AuthServiceImpl}.
 */
@RestController
@RequestMapping("/auth")
@Validated
@Tag(name = "Authentication",
     description = "Register, login, token refresh, and logout. No JWT required, except "
             + "registerAdmin once the first admin account exists (see its own docs).")
public class UserAdminController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserAdminRepository userAdminRepo;

    // ─── Register ────────────────────────────────────────────────────────────

    @Operation(summary = "Register a user",
               description = "Creates a ROLE_USER account. Returns an access token (15 min) + refresh token (7 days).")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Account created"),
        @ApiResponse(responseCode = "400", description = "Email already in use or validation failed",
                     content = @Content(schema = @Schema(implementation = UserAdminResponse.class)))
    })
    @PostMapping("/userRegister")
    public ResponseEntity<UserAdminResponse> registerUser(
            @Validated @RequestBody RegisterRequest request) {
        return toAuthResponse(authService.register(request, Role.ROLE_USER), HttpStatus.CREATED,
                "Account Created Successfully");
    }

    @Operation(summary = "Register an admin",
               description = "Creates a ROLE_ADMIN account. Reachable without a token ONLY when no admin "
                       + "account exists yet in the system (first-run bootstrap) — once at least one admin "
                       + "exists, this requires a valid ROLE_ADMIN bearer token, same as any other "
                       + "admin-only endpoint. This path stays off the gateway/security-config public-path "
                       + "list intentionally so a caller's Authorization header (if any) still reaches here "
                       + "and gets parsed into the request's authorities before this check runs.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Admin account created"),
        @ApiResponse(responseCode = "400", description = "Email already in use or validation failed"),
        @ApiResponse(responseCode = "403",
                description = "An admin already exists and the caller is not an authenticated admin")
    })
    @PostMapping("/registerAdmin")
    public ResponseEntity<?> registerAdmin(
            @Validated @RequestBody RegisterRequest request) {

        if (userAdminRepo.existsByRole(Role.ROLE_ADMIN) && !callerIsAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "success", false,
                    "message", "Admin accounts already exist. Registering another admin requires "
                            + "authenticating as an existing admin."));
        }

        return toAuthResponse(authService.register(request, Role.ROLE_ADMIN), HttpStatus.CREATED,
                "Admin Account Created Successfully");
    }

    // ─── Login ───────────────────────────────────────────────────────────────

    @Operation(summary = "Login",
               description = "Authenticates by email + password. Returns access token + refresh token.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/Loginin")
    public ResponseEntity<UserAdminResponse> login(
            @Validated @RequestBody LoginRequest request) {
        return toAuthResponse(authService.login(request), HttpStatus.OK, "Login Successful");
    }

    // ─── Refresh ─────────────────────────────────────────────────────────────

    @Operation(summary = "Refresh access token",
               description = "Exchanges a valid refresh token for a new access token. The old refresh token "
                       + "is revoked and a new one issued (rotation). Call automatically on 401.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "New tokens returned"),
        @ApiResponse(responseCode = "400", description = "Missing or malformed refreshToken"),
        @ApiResponse(responseCode = "403", description = "Token expired, revoked, or not found")
    })
    @PostMapping("/refresh-token")
    public ResponseEntity<TokenRefreshResponse> refreshToken(
            @Validated @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    // ─── Logout ──────────────────────────────────────────────────────────────

    @Operation(summary = "Logout (single session)",
               description = "Revokes the supplied refresh token. The access token expires naturally within 15 min.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Logged out"),
        @ApiResponse(responseCode = "403", description = "Refresh token not found")
    })
    @PostMapping("/logout")
    public ResponseEntity<UserAdminResponse> logout(
            @Validated @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        UserAdminResponse resp = new UserAdminResponse();
        resp.setSuccess(true);
        resp.setMessage("Logged out successfully");
        return ResponseEntity.ok(resp);
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    /**
     * True only if the current request carried a valid JWT with ROLE_ADMIN.
     * JwtValidator populates the SecurityContext from the Authorization header
     * on every request, including ones on the security-config permitAll list —
     * permitAll only means "don't reject if there's no token", it doesn't stop
     * a present token from being parsed. That's what lets an existing admin's
     * token reach this check even though the endpoint itself isn't gated at
     * the filter-chain level.
     */
    private boolean callerIsAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(Role.ROLE_ADMIN.name()));
    }

    private ResponseEntity<UserAdminResponse> toAuthResponse(
            AuthResult result, HttpStatus status, String message) {
        UserAdminResponse resp = new UserAdminResponse();
        resp.setSuccess(true);
        resp.setMessage(message);
        resp.setJwt(result.accessToken());
        resp.setRefreshToken(result.refreshToken());
        resp.setUserAdmin(UserProfileResponse.from(result.user()));
        return new ResponseEntity<>(resp, status);
    }
}
