package com.travelplatform.auth.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.travelplatform.auth.dto.UserProfileResponse;
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
     description = "Register, login, token refresh, and logout — no JWT required")
public class UserAdminController {

    @Autowired
    private AuthService authService;

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
               description = "Creates a ROLE_ADMIN account. Returns an access token + refresh token.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Admin account created"),
        @ApiResponse(responseCode = "400", description = "Email already in use or validation failed")
    })
    @PostMapping("/registerAdmin")
    public ResponseEntity<UserAdminResponse> registerAdmin(
            @Validated @RequestBody RegisterRequest request) {
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
