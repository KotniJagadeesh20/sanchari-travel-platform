package com.travelplatform.busbooking.dto;

import java.util.UUID;

/** Response for /auth/refresh-token: a fresh access token (+ rotated refresh token). */
public class TokenRefreshResponse {

	private boolean success;
	private String accessToken;
	private UUID refreshToken;
	private String message;

	public TokenRefreshResponse() {
	}

	public TokenRefreshResponse(boolean success, String accessToken, UUID refreshToken, String message) {
		this.success = success;
		this.accessToken = accessToken;
		this.refreshToken = refreshToken;
		this.message = message;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public UUID getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(UUID refreshToken) {
		this.refreshToken = refreshToken;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
