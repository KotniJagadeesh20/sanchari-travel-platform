package com.travelplatform.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.travelplatform.auth.config.JwtConstant;
import com.travelplatform.auth.entity.RefreshToken;
import com.travelplatform.auth.entity.UserAdmin;
import com.travelplatform.auth.exception.TokenRefreshException;
import com.travelplatform.auth.repository.RefreshTokenRepository;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	@InjectMocks
	private RefreshTokenService refreshTokenService;

	private UserAdmin user;
	private RefreshToken validToken;
	private UUID tokenValue;

	@BeforeEach
	void setUp() {
		user = new UserAdmin();
		user.setId(UUID.randomUUID());
		user.setEmail("test@example.com");

		tokenValue = UUID.randomUUID();

		validToken = new RefreshToken();
		validToken.setId(UUID.randomUUID());
		validToken.setToken(tokenValue);
		validToken.setUser(user);
		validToken.setCreatedAt(Instant.now());
		validToken.setExpiryDate(Instant.now().plusMillis(JwtConstant.REFRESH_TOKEN_EXPIRY_MS));
		validToken.setRevoked(false);
	}

	// ─── createRefreshToken ──────────────────────────────────────────────

	@Test
	void createRefreshToken_savesNewTokenForUser() {
		when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

		RefreshToken result = refreshTokenService.createRefreshToken(user);

		assertNotNull(result);
		assertEquals(user, result.getUser());
		assertNotNull(result.getToken(), "Token UUID must be generated");
		assertFalse(result.isRevoked(), "New token must not be revoked");
		assertTrue(result.getExpiryDate().isAfter(Instant.now()), "Expiry must be in the future");

		ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
		verify(refreshTokenRepository).save(captor.capture());
		assertEquals(user, captor.getValue().getUser());
	}

	@Test
	void createRefreshToken_eachCallProducesUniqueTokenValue() {
		when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

		RefreshToken first = refreshTokenService.createRefreshToken(user);
		RefreshToken second = refreshTokenService.createRefreshToken(user);

		assertNotEquals(first.getToken(), second.getToken(),
				"Each refresh token must be a unique UUID — never reuse");
	}

	// ─── findByToken ─────────────────────────────────────────────────────

	@Test
	void findByToken_returnsToken_whenItExists() {
		when(refreshTokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(validToken));

		Optional<RefreshToken> result = refreshTokenService.findByToken(tokenValue);

		assertTrue(result.isPresent());
		assertEquals(tokenValue, result.get().getToken());
	}

	@Test
	void findByToken_returnsEmpty_whenTokenNotFound() {
		UUID missing = UUID.randomUUID();
		when(refreshTokenRepository.findByToken(missing)).thenReturn(Optional.empty());

		Optional<RefreshToken> result = refreshTokenService.findByToken(missing);

		assertTrue(result.isEmpty());
	}

	// ─── verifyValid ─────────────────────────────────────────────────────

	@Test
	void verifyValid_returnsToken_whenTokenIsValidAndNotExpired() {
		when(refreshTokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(validToken));

		RefreshToken result = refreshTokenService.verifyValid(tokenValue);

		assertNotNull(result);
		assertEquals(tokenValue, result.getToken());
	}

	@Test
	void verifyValid_throwsTokenRefreshException_whenTokenNotFound() {
		UUID missing = UUID.randomUUID();
		when(refreshTokenRepository.findByToken(missing)).thenReturn(Optional.empty());

		TokenRefreshException ex = assertThrows(TokenRefreshException.class,
				() -> refreshTokenService.verifyValid(missing));

		assertTrue(ex.getMessage().contains("not found"));
	}

	@Test
	void verifyValid_throwsTokenRefreshException_whenTokenIsRevoked() {
		validToken.setRevoked(true);
		when(refreshTokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(validToken));

		TokenRefreshException ex = assertThrows(TokenRefreshException.class,
				() -> refreshTokenService.verifyValid(tokenValue));

		assertTrue(ex.getMessage().contains("revoked"));
	}

	@Test
	void verifyValid_throwsAndDeletesToken_whenTokenIsExpired() {
		validToken.setExpiryDate(Instant.now().minusSeconds(60));   // expired 60s ago
		when(refreshTokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(validToken));

		TokenRefreshException ex = assertThrows(TokenRefreshException.class,
				() -> refreshTokenService.verifyValid(tokenValue));

		assertTrue(ex.getMessage().contains("expired"));
		// Expired tokens must be cleaned up immediately
		verify(refreshTokenRepository).delete(validToken);
	}

	// ─── revoke ──────────────────────────────────────────────────────────

	@Test
	void revoke_setsRevokedTrueAndSaves() {
		when(refreshTokenRepository.save(validToken)).thenReturn(validToken);

		refreshTokenService.revoke(validToken);

		assertTrue(validToken.isRevoked());
		verify(refreshTokenRepository).save(validToken);
	}

	@Test
	void revoke_alreadyRevokedToken_stillSaves() {
		validToken.setRevoked(true);
		when(refreshTokenRepository.save(validToken)).thenReturn(validToken);

		// Should not throw; idempotent operation
		assertDoesNotThrow(() -> refreshTokenService.revoke(validToken));
		verify(refreshTokenRepository).save(validToken);
	}

	// ─── revokeAllForUser ────────────────────────────────────────────────

	@Test
	void revokeAllForUser_callsRepositoryMethod() {
		refreshTokenService.revokeAllForUser(user);
		verify(refreshTokenRepository).revokeAllByUser(user);
	}
}
