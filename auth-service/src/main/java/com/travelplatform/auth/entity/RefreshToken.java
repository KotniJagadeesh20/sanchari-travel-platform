package com.travelplatform.auth.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Stores a single refresh token issued to a user.
 *
 * The {@code token} column IS the refresh token value handed to the client
 * (a random UUID). Keeping it in its own table - rather than embedding it in
 * the JWT - lets the server revoke individual sessions (logout, logout-all,
 * password change, suspicious activity) without needing a JWT blacklist.
 */
@Entity
@Table(name = "refresh_token")
public class RefreshToken {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	/** The actual refresh token value sent to / received from the client. */
	@Column(nullable = false, unique = true)
	private UUID token;

	@ManyToOne
	@JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
	private UserAdmin user;

	@Column(nullable = false)
	private Instant expiryDate;

	@Column(nullable = false)
	private Instant createdAt;

	/** Set to true on logout / rotation so the token can never be reused. */
	@Column(nullable = false)
	private boolean revoked = false;

	public RefreshToken() {
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public UUID getToken() {
		return token;
	}

	public void setToken(UUID token) {
		this.token = token;
	}

	public UserAdmin getUser() {
		return user;
	}

	public void setUser(UserAdmin user) {
		this.user = user;
	}

	public Instant getExpiryDate() {
		return expiryDate;
	}

	public void setExpiryDate(Instant expiryDate) {
		this.expiryDate = expiryDate;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public boolean isRevoked() {
		return revoked;
	}

	public void setRevoked(boolean revoked) {
		this.revoked = revoked;
	}
}
