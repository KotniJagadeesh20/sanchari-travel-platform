package com.travelplatform.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.travelplatform.auth.service.CustomDetailsImpl;
import com.travelplatform.auth.config.JwtProvider;
import com.travelplatform.auth.dto.LoginRequest;
import com.travelplatform.auth.dto.RefreshTokenRequest;
import com.travelplatform.auth.dto.RegisterRequest;
import com.travelplatform.auth.dto.TokenRefreshResponse;
import com.travelplatform.auth.entity.RefreshToken;
import com.travelplatform.auth.entity.UserAdmin;
import com.travelplatform.auth.enums.Role;
import com.travelplatform.auth.exception.EmailAlreadyExistsException;
import com.travelplatform.auth.exception.TokenRefreshException;
import com.travelplatform.auth.repository.UserAdminRepository;
import com.travelplatform.auth.service.AuthService.AuthResult;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserAdminRepository userAdminRepo;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private CustomDetailsImpl customDetailsImpl;
    @Mock private JwtProvider jwtProvider;
    @Mock private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthServiceImpl authService;

    // ─── shared fixtures ────────────────────────────────────────────────────

    private RegisterRequest validRegisterRequest;
    private UserAdmin savedUser;
    private UserDetails userDetails;
    private RefreshToken refreshToken;

    @BeforeEach
    void setUp() {
        validRegisterRequest = new RegisterRequest();
        validRegisterRequest.setEmail("asha@example.com");
        validRegisterRequest.setPassword("Passw0rd!");
        validRegisterRequest.setName("Asha Kumar");
        validRegisterRequest.setPhone("9876543210");
        validRegisterRequest.setGender("Female");
        validRegisterRequest.setAge(29);

        savedUser = new UserAdmin();
        savedUser.setId(UUID.randomUUID());
        savedUser.setEmail("asha@example.com");
        savedUser.setPassword("encoded");
        savedUser.setRole(Role.ROLE_USER);

        userDetails = new User("asha@example.com", "encoded",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        refreshToken = new RefreshToken();
        refreshToken.setId(UUID.randomUUID());
        refreshToken.setToken(UUID.randomUUID());
        refreshToken.setUser(savedUser);
        refreshToken.setExpiryDate(Instant.now().plusSeconds(600));
        refreshToken.setRevoked(false);
    }

    // ════════════════════════════════════════════════════════════════════════
    // register
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    class Register {

        @Test
        void register_returnsAuthResult_withAccessAndRefreshTokens() {
            when(userAdminRepo.findByEmail("asha@example.com")).thenReturn(null);
            when(passwordEncoder.encode("Passw0rd!")).thenReturn("encoded");
            when(userAdminRepo.save(any(UserAdmin.class))).thenReturn(savedUser);
            when(customDetailsImpl.loadUserByUsername("asha@example.com")).thenReturn(userDetails);
            when(jwtProvider.generateToken(any())).thenReturn("access.token");
            when(refreshTokenService.createRefreshToken(savedUser)).thenReturn(refreshToken);

            AuthResult result = authService.register(validRegisterRequest, Role.ROLE_USER);

            assertNotNull(result);
            assertEquals("access.token", result.accessToken());
            assertEquals(refreshToken.getToken(), result.refreshToken());
            assertEquals(savedUser, result.user());
        }

        @Test
        void register_encodesPasswordBeforeSaving() {
            when(userAdminRepo.findByEmail(anyString())).thenReturn(null);
            when(passwordEncoder.encode("Passw0rd!")).thenReturn("bcrypt-hash");
            when(userAdminRepo.save(any(UserAdmin.class))).thenReturn(savedUser);
            when(customDetailsImpl.loadUserByUsername(anyString())).thenReturn(userDetails);
            when(jwtProvider.generateToken(any())).thenReturn("access.token");
            when(refreshTokenService.createRefreshToken(any())).thenReturn(refreshToken);

            authService.register(validRegisterRequest, Role.ROLE_USER);

            ArgumentCaptor<UserAdmin> captor = ArgumentCaptor.forClass(UserAdmin.class);
            verify(userAdminRepo).save(captor.capture());
            assertEquals("bcrypt-hash", captor.getValue().getPassword(),
                    "Raw password must never be persisted");
        }

        @Test
        void register_assignsCorrectRole() {
            when(userAdminRepo.findByEmail(anyString())).thenReturn(null);
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            when(userAdminRepo.save(any(UserAdmin.class))).thenReturn(savedUser);
            when(customDetailsImpl.loadUserByUsername(anyString())).thenReturn(userDetails);
            when(jwtProvider.generateToken(any())).thenReturn("t");
            when(refreshTokenService.createRefreshToken(any())).thenReturn(refreshToken);

            authService.register(validRegisterRequest, Role.ROLE_ADMIN);

            ArgumentCaptor<UserAdmin> captor = ArgumentCaptor.forClass(UserAdmin.class);
            verify(userAdminRepo).save(captor.capture());
            assertEquals(Role.ROLE_ADMIN, captor.getValue().getRole());
        }

        @Test
        void register_throwsEmailAlreadyExists_whenEmailTaken() {
            when(userAdminRepo.findByEmail("asha@example.com")).thenReturn(savedUser);

            assertThrows(EmailAlreadyExistsException.class,
                    () -> authService.register(validRegisterRequest, Role.ROLE_USER));

            verify(userAdminRepo, never()).save(any());
            verifyNoInteractions(jwtProvider, refreshTokenService);
        }

        @Test
        void register_neverCreatesRefreshToken_whenEmailTaken() {
            when(userAdminRepo.findByEmail(anyString())).thenReturn(savedUser);

            assertThrows(EmailAlreadyExistsException.class,
                    () -> authService.register(validRegisterRequest, Role.ROLE_USER));

            verifyNoInteractions(refreshTokenService);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // login
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    class Login {

        private LoginRequest loginRequest;

        @BeforeEach
        void setUp() {
            loginRequest = new LoginRequest();
            loginRequest.setEmail("asha@example.com");
            loginRequest.setPassword("Passw0rd!");
        }

        @Test
        void login_returnsAuthResult_onValidCredentials() {
            when(customDetailsImpl.loadUserByUsername("asha@example.com")).thenReturn(userDetails);
            when(passwordEncoder.matches("Passw0rd!", "encoded")).thenReturn(true);
            when(jwtProvider.generateToken(any())).thenReturn("access.token");
            when(userAdminRepo.findByEmail("asha@example.com")).thenReturn(savedUser);
            when(refreshTokenService.createRefreshToken(savedUser)).thenReturn(refreshToken);

            AuthResult result = authService.login(loginRequest);

            assertEquals("access.token", result.accessToken());
            assertEquals(refreshToken.getToken(), result.refreshToken());
            assertEquals(savedUser, result.user());
        }

        @Test
        void login_throwsBadCredentials_whenPasswordWrong() {
            when(customDetailsImpl.loadUserByUsername("asha@example.com")).thenReturn(userDetails);
            when(passwordEncoder.matches("Passw0rd!", "encoded")).thenReturn(false);

            assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));

            verifyNoInteractions(jwtProvider, refreshTokenService);
        }

        @Test
        void login_throwsBadCredentials_whenEmailNotFound() {
            when(customDetailsImpl.loadUserByUsername("ghost@example.com"))
                    .thenThrow(new org.springframework.security.core.userdetails.UsernameNotFoundException("not found"));

            loginRequest.setEmail("ghost@example.com");

            assertThrows(org.springframework.security.core.userdetails.UsernameNotFoundException.class,
                    () -> authService.login(loginRequest));

            verifyNoInteractions(jwtProvider, refreshTokenService);
        }

        @Test
        void login_issuesNewRefreshToken_onEverySuccessfulLogin() {
            when(customDetailsImpl.loadUserByUsername(anyString())).thenReturn(userDetails);
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(jwtProvider.generateToken(any())).thenReturn("token");
            when(userAdminRepo.findByEmail(anyString())).thenReturn(savedUser);

            RefreshToken first = new RefreshToken();
            first.setToken(UUID.randomUUID());
            RefreshToken second = new RefreshToken();
            second.setToken(UUID.randomUUID());

            when(refreshTokenService.createRefreshToken(savedUser))
                    .thenReturn(first)
                    .thenReturn(second);

            AuthResult r1 = authService.login(loginRequest);
            AuthResult r2 = authService.login(loginRequest);

            assertNotEquals(r1.refreshToken(), r2.refreshToken(),
                    "Each login must produce a unique refresh token");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // refresh
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    class Refresh {

        private RefreshTokenRequest request;

        @BeforeEach
        void setUp() {
            request = new RefreshTokenRequest();
            request.setRefreshToken(refreshToken.getToken());
        }

        @Test
        void refresh_returnsNewAccessAndRefreshTokens_whenTokenValid() {
            when(refreshTokenService.verifyValid(refreshToken.getToken())).thenReturn(refreshToken);
            when(customDetailsImpl.loadUserByUsername("asha@example.com")).thenReturn(userDetails);
            when(jwtProvider.generateToken(any())).thenReturn("new.access.token");

            RefreshToken rotated = new RefreshToken();
            rotated.setToken(UUID.randomUUID());
            when(refreshTokenService.createRefreshToken(savedUser)).thenReturn(rotated);

            TokenRefreshResponse response = authService.refresh(request);

            assertTrue(response.isSuccess());
            assertEquals("new.access.token", response.getAccessToken());
            assertEquals(rotated.getToken(), response.getRefreshToken());
        }

        @Test
        void refresh_revokesOldToken_beforeIssuingNew() {
            when(refreshTokenService.verifyValid(refreshToken.getToken())).thenReturn(refreshToken);
            when(customDetailsImpl.loadUserByUsername(anyString())).thenReturn(userDetails);
            when(jwtProvider.generateToken(any())).thenReturn("t");
            RefreshToken rotated = new RefreshToken();
            rotated.setToken(UUID.randomUUID());
            when(refreshTokenService.createRefreshToken(any())).thenReturn(rotated);

            authService.refresh(request);

            verify(refreshTokenService).revoke(refreshToken);
        }

        @Test
        void refresh_neverReusesSameRefreshToken() {
            when(refreshTokenService.verifyValid(refreshToken.getToken())).thenReturn(refreshToken);
            when(customDetailsImpl.loadUserByUsername(anyString())).thenReturn(userDetails);
            when(jwtProvider.generateToken(any())).thenReturn("t");
            RefreshToken rotated = new RefreshToken();
            rotated.setToken(UUID.randomUUID());
            when(refreshTokenService.createRefreshToken(any())).thenReturn(rotated);

            TokenRefreshResponse response = authService.refresh(request);

            assertNotEquals(refreshToken.getToken(), response.getRefreshToken(),
                    "Rotated refresh token must differ from the consumed one");
        }

        @Test
        void refresh_propagatesTokenRefreshException_fromVerify() {
            when(refreshTokenService.verifyValid(any()))
                    .thenThrow(new TokenRefreshException("Expired."));

            assertThrows(TokenRefreshException.class, () -> authService.refresh(request));
            verifyNoInteractions(jwtProvider);
        }

        @Test
        void refresh_propagatesTokenRefreshException_forRevokedToken() {
            when(refreshTokenService.verifyValid(any()))
                    .thenThrow(new TokenRefreshException("Refresh token was revoked."));

            assertThrows(TokenRefreshException.class, () -> authService.refresh(request));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // logout
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    class Logout {

        private RefreshTokenRequest request;

        @BeforeEach
        void setUp() {
            request = new RefreshTokenRequest();
            request.setRefreshToken(refreshToken.getToken());
        }

        @Test
        void logout_revokesToken_whenFound() {
            when(refreshTokenService.findByToken(refreshToken.getToken()))
                    .thenReturn(Optional.of(refreshToken));

            assertDoesNotThrow(() -> authService.logout(request));

            verify(refreshTokenService).revoke(refreshToken);
        }

        @Test
        void logout_throwsTokenRefreshException_whenTokenNotFound() {
            when(refreshTokenService.findByToken(any())).thenReturn(Optional.empty());

            assertThrows(TokenRefreshException.class, () -> authService.logout(request));

            verify(refreshTokenService, never()).revoke(any());
        }

        @Test
        void logout_isIdempotentConcept_revokeCalledOnce() {
            when(refreshTokenService.findByToken(refreshToken.getToken()))
                    .thenReturn(Optional.of(refreshToken));

            authService.logout(request);

            verify(refreshTokenService, times(1)).revoke(refreshToken);
        }
    }
}
