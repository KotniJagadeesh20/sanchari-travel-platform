package com.travelplatform.auth.service;

import java.util.UUID;

import com.travelplatform.auth.dto.LoginRequest;
import com.travelplatform.auth.dto.RefreshTokenRequest;
import com.travelplatform.auth.dto.RegisterRequest;
import com.travelplatform.auth.dto.TokenRefreshResponse;
import com.travelplatform.auth.entity.UserAdmin;
import com.travelplatform.auth.enums.Role;

/**
 * Centralises all authentication business logic so that the controller
 * stays thin (HTTP in / HTTP out) and auth behaviour can be unit-tested
 * without MockMvc.
 */
public interface AuthService {

    /**
     * Register a new account with the given role.
     * Throws {@link com.travelplatform.auth.exception.EmailAlreadyExistsException}
     * if the email is taken.
     */
    AuthResult register(RegisterRequest request, Role role);

    /**
     * Authenticate by email + password.
     * Throws {@link org.springframework.security.authentication.BadCredentialsException}
     * on failure.
     */
    AuthResult login(LoginRequest request);

    /**
     * Exchange a valid refresh token for a new access token + rotated refresh token.
     * Throws {@link com.travelplatform.auth.exception.TokenRefreshException}
     * if the token is invalid, expired, or revoked.
     */
    TokenRefreshResponse refresh(RefreshTokenRequest request);

    /**
     * Revoke the supplied refresh token (single-session logout).
     * Throws {@link com.travelplatform.auth.exception.TokenRefreshException}
     * if the token is not found.
     */
    void logout(RefreshTokenRequest request);

    /** Holds the tokens returned after a successful register or login. */
    record AuthResult(String accessToken, java.util.UUID refreshToken, UserAdmin user) {}
}
