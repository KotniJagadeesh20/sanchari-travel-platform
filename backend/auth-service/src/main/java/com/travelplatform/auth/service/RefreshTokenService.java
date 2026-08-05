package com.travelplatform.auth.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.travelplatform.auth.config.JwtConstant;
import com.travelplatform.auth.entity.RefreshToken;
import com.travelplatform.auth.entity.UserAdmin;
import com.travelplatform.auth.exception.TokenRefreshException;
import com.travelplatform.auth.repository.RefreshTokenRepository;

@Service
public class RefreshTokenService {

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	/**
	 * Issues a brand-new refresh token row for the given user.
	 * Any previous tokens for this user are left untouched here -
	 * call {@link #revokeAllForUser(UserAdmin)} first if you want
	 * single-session-per-user behaviour.
	 */
	@Transactional
	public RefreshToken createRefreshToken(UserAdmin user) {
		RefreshToken refreshToken = new RefreshToken();
		refreshToken.setUser(user);
		refreshToken.setToken(UUID.randomUUID());
		refreshToken.setCreatedAt(Instant.now());
		refreshToken.setExpiryDate(Instant.now().plusMillis(JwtConstant.REFRESH_TOKEN_EXPIRY_MS));
		refreshToken.setRevoked(false);
		return refreshTokenRepository.save(refreshToken);
	}

	public Optional<RefreshToken> findByToken(UUID token) {
		return refreshTokenRepository.findByToken(token);
	}

	/**
	 * Validates that a refresh token exists, is not revoked, and has not expired.
	 * Throws {@link TokenRefreshException} otherwise (mapped to HTTP 403).
	 */
	public RefreshToken verifyValid(UUID token) {
		RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
				.orElseThrow(() -> new TokenRefreshException("Refresh token not found. Please log in again."));

		if (refreshToken.isRevoked()) {
			throw new TokenRefreshException("Refresh token was revoked. Please log in again.");
		}

		if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
			refreshTokenRepository.delete(refreshToken);
			throw new TokenRefreshException("Refresh token expired. Please log in again.");
		}

		return refreshToken;
	}

	/** Revokes a single refresh token (used on /auth/logout). */
	@Transactional
	public void revoke(RefreshToken refreshToken) {
		refreshToken.setRevoked(true);
		refreshTokenRepository.save(refreshToken);
	}

	/** Revokes every refresh token belonging to a user (used on logout-all / password change). */
	@Transactional
	public void revokeAllForUser(UserAdmin user) {
		refreshTokenRepository.revokeAllByUser(user);
	}
}
