package com.travelplatform.auth.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.travelplatform.auth.dto.TokenRefreshResponse;
import com.travelplatform.auth.entity.UserAdmin;
import com.travelplatform.auth.enums.Role;
import com.travelplatform.auth.exception.EmailAlreadyExistsException;
import com.travelplatform.auth.exception.GlobalExceptionHandler;
import com.travelplatform.auth.exception.TokenRefreshException;
import com.travelplatform.auth.service.AuthService;
import com.travelplatform.auth.service.AuthService.AuthResult;

/**
 * Thin controller tests: verifies HTTP status codes, JSON shape, and that
 * the controller correctly delegates to AuthService.
 *
 * Business logic (credential checks, token rotation, etc.) is tested
 * in {@link com.travelplatform.auth.service.AuthServiceImplTest}.
 */
@ExtendWith(MockitoExtension.class)
class UserAdminControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private UserAdminController controller;

    private MockMvc mockMvc;

    // ─── JSON payloads ───────────────────────────────────────────────────────

    private static final String VALID_REGISTER_JSON = """
            {
              "name": "Asha Kumar",
              "email": "asha@example.com",
              "password": "Passw0rd!",
              "phone": "9876543210",
              "dob": "1995-06-15",
              "gender": "Female",
              "age": 29
            }
            """;

    private static final String LOGIN_JSON = """
            {"email":"asha@example.com","password":"Passw0rd!"}
            """;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(new LocalValidatorFactoryBean())
                .build();
    }

    // ════════════════════════════════════════════════════════════════════════
    // /auth/userRegister
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    class RegisterUser {

        private AuthResult successResult;

        @BeforeEach
        void setUp() {
            UserAdmin user = new UserAdmin();
            user.setId(UUID.randomUUID());
            user.setEmail("asha@example.com");
            user.setRole(Role.ROLE_USER);
            successResult = new AuthResult("access.jwt", UUID.randomUUID(), user);
        }

        @Test
        void returns201_andTokens_whenRegistrationSucceeds() throws Exception {
            when(authService.register(any(), eq(Role.ROLE_USER))).thenReturn(successResult);

            mockMvc.perform(post("/auth/userRegister")
                            .contentType("application/json")
                            .content(VALID_REGISTER_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.jwt", is("access.jwt")))
                    .andExpect(jsonPath("$.refreshToken", is(successResult.refreshToken().toString())));
        }

        @Test
        void returns400_whenEmailAlreadyTaken() throws Exception {
            when(authService.register(any(), any()))
                    .thenThrow(new EmailAlreadyExistsException("asha@example.com"));

            mockMvc.perform(post("/auth/userRegister")
                            .contentType("application/json")
                            .content(VALID_REGISTER_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)));
        }

        @Test
        void returns400_withFieldErrors_whenPasswordTooWeak() throws Exception {
            String weak = VALID_REGISTER_JSON.replace("Passw0rd!", "weak");

            mockMvc.perform(post("/auth/userRegister")
                            .contentType("application/json")
                            .content(weak))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.password").exists());

            verifyNoInteractions(authService);
        }

        @Test
        void returns400_withFieldErrors_whenEmailMalformed() throws Exception {
            String bad = VALID_REGISTER_JSON.replace("asha@example.com", "not-an-email");

            mockMvc.perform(post("/auth/userRegister")
                            .contentType("application/json")
                            .content(bad))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.email").exists());

            verifyNoInteractions(authService);
        }

        @Test
        void returns400_withFieldErrors_whenPhoneInvalid() throws Exception {
            String bad = VALID_REGISTER_JSON.replace("9876543210", "12345");

            mockMvc.perform(post("/auth/userRegister")
                            .contentType("application/json")
                            .content(bad))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.phone").exists());

            verifyNoInteractions(authService);
        }

        @Test
        void delegatesToAuthService_withRoleUser() throws Exception {
            when(authService.register(any(), eq(Role.ROLE_USER))).thenReturn(successResult);

            mockMvc.perform(post("/auth/userRegister")
                    .contentType("application/json").content(VALID_REGISTER_JSON));

            verify(authService).register(any(), eq(Role.ROLE_USER));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // /auth/registerAdmin
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    class RegisterAdmin {

        @Test
        void delegatesToAuthService_withRoleAdmin() throws Exception {
            UserAdmin admin = new UserAdmin();
            admin.setId(UUID.randomUUID());
            admin.setRole(Role.ROLE_ADMIN);
            AuthResult result = new AuthResult("access.jwt", UUID.randomUUID(), admin);

            when(authService.register(any(), eq(Role.ROLE_ADMIN))).thenReturn(result);

            mockMvc.perform(post("/auth/registerAdmin")
                            .contentType("application/json")
                            .content(VALID_REGISTER_JSON))
                    .andExpect(status().isCreated());

            verify(authService).register(any(), eq(Role.ROLE_ADMIN));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // /auth/Loginin
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    class Login {

        @Test
        void returns200_andTokens_onValidCredentials() throws Exception {
            UserAdmin user = new UserAdmin();
            user.setId(UUID.randomUUID());
            user.setEmail("asha@example.com");
            UUID rt = UUID.randomUUID();
            AuthResult result = new AuthResult("access.jwt", rt, user);
            when(authService.login(any())).thenReturn(result);

            mockMvc.perform(post("/auth/Loginin")
                            .contentType("application/json")
                            .content(LOGIN_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.jwt", is("access.jwt")))
                    .andExpect(jsonPath("$.refreshToken", is(rt.toString())));
        }

        @Test
        void returns401_whenBadCredentials() throws Exception {
            when(authService.login(any()))
                    .thenThrow(new BadCredentialsException("Incorrect password"));

            mockMvc.perform(post("/auth/Loginin")
                            .contentType("application/json")
                            .content(LOGIN_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success", is(false)));
        }

        @Test
        void returns400_whenEmailBlank() throws Exception {
            mockMvc.perform(post("/auth/Loginin")
                            .contentType("application/json")
                            .content("{\"email\":\"\",\"password\":\"Passw0rd!\"}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(authService);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // /auth/refresh-token
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    class RefreshToken {

        private UUID tokenValue;
        private String requestJson;

        @BeforeEach
        void setUp() {
            tokenValue = UUID.randomUUID();
            requestJson = "{\"refreshToken\":\"" + tokenValue + "\"}";
        }

        @Test
        void returns200_withNewTokens_whenTokenValid() throws Exception {
            UUID newRt = UUID.randomUUID();
            TokenRefreshResponse resp = new TokenRefreshResponse(true, "new.access", newRt, "ok");
            when(authService.refresh(any())).thenReturn(resp);

            mockMvc.perform(post("/auth/refresh-token")
                            .contentType("application/json")
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.accessToken", is("new.access")))
                    .andExpect(jsonPath("$.refreshToken", is(newRt.toString())));
        }

        @Test
        void returns403_whenTokenExpiredOrRevoked() throws Exception {
            when(authService.refresh(any()))
                    .thenThrow(new TokenRefreshException("Refresh token expired. Please log in again."));

            mockMvc.perform(post("/auth/refresh-token")
                            .contentType("application/json")
                            .content(requestJson))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.message", containsString("expired")));
        }

        @Test
        void returns400_whenRefreshTokenFieldMissing() throws Exception {
            mockMvc.perform(post("/auth/refresh-token")
                            .contentType("application/json")
                            .content("{}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(authService);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // /auth/logout
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    class Logout {

        private UUID tokenValue;
        private String requestJson;

        @BeforeEach
        void setUp() {
            tokenValue = UUID.randomUUID();
            requestJson = "{\"refreshToken\":\"" + tokenValue + "\"}";
        }

        @Test
        void returns200_whenLogoutSucceeds() throws Exception {
            doNothing().when(authService).logout(any());

            mockMvc.perform(post("/auth/logout")
                            .contentType("application/json")
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)));
        }

        @Test
        void returns403_whenRefreshTokenNotFound() throws Exception {
            doThrow(new TokenRefreshException("Refresh token not found."))
                    .when(authService).logout(any());

            mockMvc.perform(post("/auth/logout")
                            .contentType("application/json")
                            .content(requestJson))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success", is(false)));
        }

        @Test
        void returns400_whenRefreshTokenFieldMissing() throws Exception {
            mockMvc.perform(post("/auth/logout")
                            .contentType("application/json")
                            .content("{}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(authService);
        }

        @Test
        void delegatesToAuthService_logout() throws Exception {
            doNothing().when(authService).logout(any());

            mockMvc.perform(post("/auth/logout")
                    .contentType("application/json").content(requestJson));

            verify(authService).logout(any());
        }
    }
}
