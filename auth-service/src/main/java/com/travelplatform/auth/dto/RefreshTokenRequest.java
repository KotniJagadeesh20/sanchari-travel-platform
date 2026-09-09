package com.travelplatform.auth.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/** Body for /auth/refresh-token and /auth/logout. */
@Schema(description = "Refresh token UUID (received from /auth/Loginin or /auth/refresh-token)")
public class RefreshTokenRequest {

	@NotNull(message = "refreshToken is required")
	@Schema(description = "The refresh token UUID issued at login", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") private UUID refreshToken;

	public UUID getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(UUID refreshToken) {
		this.refreshToken = refreshToken;
	}
}
