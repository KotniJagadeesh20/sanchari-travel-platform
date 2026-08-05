package com.travelplatform.auth.exception;

/**
 * Thrown when a refresh token is missing, expired, or has been revoked.
 * Handled by {@link GlobalExceptionHandler}, which maps it to HTTP 403.
 */
public class TokenRefreshException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public TokenRefreshException(String message) {
		super(message);
	}
}
