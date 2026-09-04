package com.travelplatform.auth.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.travelplatform.auth.entity.UserAdmin;
import com.travelplatform.auth.enums.Role;
import com.travelplatform.auth.exception.GlobalExceptionHandler;
import com.travelplatform.auth.repository.UserAdminRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;

@ExtendWith(MockitoExtension.class)
class UserProfileControllerTest {

    @Mock private UserAdminRepository userAdminRepo;
    @InjectMocks private UserProfileController userProfileController;

    private MockMvc mockMvc;
    private UserAdmin user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userProfileController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        userId = UUID.randomUUID();
        user = new UserAdmin();
        user.setId(userId);
        user.setEmail("asha@example.com");
        user.setName("Asha Kumar");
        user.setPhone("9876543210");
        user.setGender("Female");
        user.setAge(29);
        user.setRole(Role.ROLE_USER);
        user.setPassword("encoded-password-never-returned");
    }

    @Nested
    class GetAllUsersForAdmin {

        @Test
        void returns200_withEveryUser() throws Exception {
            UserAdmin second = new UserAdmin();
            second.setId(UUID.randomUUID());
            second.setName("Second User");
            second.setEmail("second@example.com");
            second.setPhone("9123456780");
            second.setGender("Male");
            second.setAge(40);
            second.setRole(Role.ROLE_ADMIN);

            when(userAdminRepo.findAll()).thenReturn(java.util.List.of(user, second));

            mockMvc.perform(get("/auth/users/admin"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()", is(2)))
                    .andExpect(jsonPath("$[1].role", is("ROLE_ADMIN")));
        }

        @Test
        void neverExposesPassword() throws Exception {
            when(userAdminRepo.findAll()).thenReturn(java.util.List.of(user));

            mockMvc.perform(get("/auth/users/admin"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].password").doesNotExist());
        }
    }

    @Nested
    class GetById {

        @Test
        void returns200_withSafeFields_whenUserExists() throws Exception {
            when(userAdminRepo.findById(userId)).thenReturn(Optional.of(user));

            mockMvc.perform(get("/auth/users/" + userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(userId.toString())))
                    .andExpect(jsonPath("$.email", is("asha@example.com")))
                    .andExpect(jsonPath("$.name", is("Asha Kumar")))
                    .andExpect(jsonPath("$.role", is("ROLE_USER")));
        }

        @Test
        void neverExposesPassword() throws Exception {
            when(userAdminRepo.findById(userId)).thenReturn(Optional.of(user));

            mockMvc.perform(get("/auth/users/" + userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.password").doesNotExist());
        }

        @Test
        void returns404_whenUserNotFound() throws Exception {
            UUID missingId = UUID.randomUUID();
            when(userAdminRepo.findById(missingId)).thenReturn(Optional.empty());

            mockMvc.perform(get("/auth/users/" + missingId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success", is(false)));
        }
    }

    @Nested
    class GetByEmail {

        @Test
        void returns200_withProfile_whenEmailFound() throws Exception {
            when(userAdminRepo.findByEmail("asha@example.com")).thenReturn(user);

            mockMvc.perform(get("/auth/users/by-email/asha@example.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email", is("asha@example.com")))
                    .andExpect(jsonPath("$.id", is(userId.toString())));
        }

        @Test
        void neverExposesPassword_onEmailLookup() throws Exception {
            when(userAdminRepo.findByEmail("asha@example.com")).thenReturn(user);

            mockMvc.perform(get("/auth/users/by-email/asha@example.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.password").doesNotExist());
        }

        @Test
        void returns404_whenEmailNotFound() throws Exception {
            when(userAdminRepo.findByEmail("missing@example.com")).thenReturn(null);

            mockMvc.perform(get("/auth/users/by-email/missing@example.com"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success", is(false)));
        }
    }

    @Nested
    class UpdateMyProfile {

        private final ObjectMapper objectMapper = new ObjectMapper();

        @Test
        void appliesOnlyProvidedFields_leavesOthersUnchanged() throws Exception {
            when(userAdminRepo.findById(userId)).thenReturn(Optional.of(user));
            when(userAdminRepo.save(any(UserAdmin.class))).thenAnswer(i -> i.getArgument(0));

            mockMvc.perform(put("/auth/users/me")
                    .header("X-Authenticated-User-Id", userId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(java.util.Map.of("name", "Asha K. Reddy"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name", is("Asha K. Reddy")))
                    .andExpect(jsonPath("$.phone", is("9876543210"))); // unchanged
        }

        @Test
        void neverAcceptsEmailOrPassword_fieldsSimplyDontExistOnTheRequestDto() throws Exception {
            when(userAdminRepo.findById(userId)).thenReturn(Optional.of(user));
            when(userAdminRepo.save(any(UserAdmin.class))).thenAnswer(i -> i.getArgument(0));

            mockMvc.perform(put("/auth/users/me")
                    .header("X-Authenticated-User-Id", userId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"hacked@example.com\",\"password\":\"newpass\",\"name\":\"Asha K. Reddy\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email", is("asha@example.com"))); // unchanged — email isn't a bindable field
        }

        @Test
        void alwaysUpdatesTheCallerFromTheHeader_neverATargetInTheBody() throws Exception {
            UUID someoneElsesId = UUID.randomUUID();
            when(userAdminRepo.findById(userId)).thenReturn(Optional.of(user));
            when(userAdminRepo.save(any(UserAdmin.class))).thenAnswer(i -> i.getArgument(0));

            mockMvc.perform(put("/auth/users/me")
                    .header("X-Authenticated-User-Id", userId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(java.util.Map.of("name", "Still Me"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(userId.toString())));

            verify(userAdminRepo, never()).findById(someoneElsesId);
        }

        @Test
        void returns404_whenAuthenticatedUserSomehowNoLongerExists() throws Exception {
            UUID missingId = UUID.randomUUID();

            mockMvc.perform(put("/auth/users/me")
                    .header("X-Authenticated-User-Id", missingId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success", is(false)));
        }

        @Test
        void neverExposesPassword_onUpdateResponse() throws Exception {
            when(userAdminRepo.findById(userId)).thenReturn(Optional.of(user));
            when(userAdminRepo.save(any(UserAdmin.class))).thenAnswer(i -> i.getArgument(0));

            mockMvc.perform(put("/auth/users/me")
                    .header("X-Authenticated-User-Id", userId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.password").doesNotExist());
        }
    }
}
