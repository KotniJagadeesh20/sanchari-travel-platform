package com.travelplatform.auth.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
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
}
