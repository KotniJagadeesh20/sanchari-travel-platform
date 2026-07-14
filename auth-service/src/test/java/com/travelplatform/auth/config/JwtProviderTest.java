package com.travelplatform.auth.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.travelplatform.auth.entity.UserAdmin;
import com.travelplatform.auth.repository.UserAdminRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;

@ExtendWith(MockitoExtension.class)
class JwtProviderTest {

	@Mock
	private UserAdminRepository userAdminRepo;

	@InjectMocks
	private JwtProvider jwtProvider;

	private UUID johnId;

	@BeforeEach
	void setUp() {
		johnId = UUID.randomUUID();
	}

	private UserAdmin buildUser(UUID id, String email, String name) {
		UserAdmin user = new UserAdmin();
		user.setId(id);
		user.setEmail(email);
		user.setName(name);
		return user;
	}

	@Test
	void generateToken_includesEmailNameAndAuthorities() {
		when(userAdminRepo.findByEmail("john@example.com"))
				.thenReturn(buildUser(johnId, "john@example.com", "John Doe"));

		Authentication auth = new UsernamePasswordAuthenticationToken(
				"john@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));

		String token = jwtProvider.generateToken(auth);

		assertNotNull(token);

		SecretKey key = Keys.hmacShaKeyFor(JwtConstant.SECRET_KEY.getBytes());
		Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();

		assertEquals("john@example.com", claims.get("email"));
		assertEquals("John Doe", claims.get("name"));
		assertEquals("ROLE_USER", claims.get("authorities"));
		assertEquals(johnId.toString(), claims.get("userId"));
	}

	@Test
	void generateToken_withMultipleAuthorities_joinsWithComma() {
		when(userAdminRepo.findByEmail("admin@example.com"))
				.thenReturn(buildUser(UUID.randomUUID(), "admin@example.com", "Admin User"));

		Authentication auth = new UsernamePasswordAuthenticationToken(
				"admin@example.com", null,
				List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER")));

		String token = jwtProvider.generateToken(auth);

		SecretKey key = Keys.hmacShaKeyFor(JwtConstant.SECRET_KEY.getBytes());
		Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();

		assertEquals("ROLE_ADMIN,ROLE_USER", claims.get("authorities"));
	}

	@Test
	void generateToken_whenUserNotFound_omitsNameAndUserId() {
		when(userAdminRepo.findByEmail("ghost@example.com")).thenReturn(null);

		Authentication auth = new UsernamePasswordAuthenticationToken(
				"ghost@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));

		String token = jwtProvider.generateToken(auth);

		SecretKey key = Keys.hmacShaKeyFor(JwtConstant.SECRET_KEY.getBytes());
		Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();

		assertEquals("", claims.get("name"));
		assertEquals("", claims.get("userId"));
	}

	@Test
	void getEmailFromToken_returnsEmailClaim() {
		when(userAdminRepo.findByEmail("jane@example.com"))
				.thenReturn(buildUser(UUID.randomUUID(), "jane@example.com", "Jane Doe"));

		Authentication auth = new UsernamePasswordAuthenticationToken(
				"jane@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));

		String token = jwtProvider.generateToken(auth);

		// JwtProvider expects the "Bearer " prefix (it strips the first 7 chars)
		String email = jwtProvider.getEmailFromToken("Bearer " + token);

		assertEquals("jane@example.com", email);
	}
}
