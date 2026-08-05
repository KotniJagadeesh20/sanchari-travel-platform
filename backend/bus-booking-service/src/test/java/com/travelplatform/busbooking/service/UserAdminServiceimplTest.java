package com.travelplatform.busbooking.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.travelplatform.busbooking.entity.UserAdmin;
import com.travelplatform.busbooking.repository.UserAdminRepository;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceimplTest {

    @Mock private UserAdminRepository userRepo;
    @InjectMocks private UserAdminServiceimpl userAdminService;

    private UUID userId;
    private String email;
    private UserAdmin existingRef;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        email = "asha@example.com";
        existingRef = new UserAdmin(userId, email);
    }

    @Test
    void findOrCreate_returnsExisting_whenAlreadyPresent() {
        when(userRepo.findById(userId)).thenReturn(Optional.of(existingRef));
        UserAdmin result = userAdminService.findOrCreate(userId, email);
        assertEquals(existingRef, result);
        verify(userRepo, never()).save(any());
    }

    @Test
    void findOrCreate_createsAndSaves_whenNotPresent() {
        when(userRepo.findById(userId)).thenReturn(Optional.empty());
        when(userRepo.save(any(UserAdmin.class))).thenAnswer(i -> i.getArgument(0));
        UserAdmin result = userAdminService.findOrCreate(userId, email);
        assertNotNull(result);
        assertEquals(userId, result.getId());
        assertEquals(email, result.getEmail());
        verify(userRepo).save(any(UserAdmin.class));
    }

    @Test
    void findOrCreate_savesCorrectValues() {
        when(userRepo.findById(userId)).thenReturn(Optional.empty());
        when(userRepo.save(any(UserAdmin.class))).thenAnswer(i -> i.getArgument(0));
        userAdminService.findOrCreate(userId, email);
        ArgumentCaptor<UserAdmin> captor = ArgumentCaptor.forClass(UserAdmin.class);
        verify(userRepo).save(captor.capture());
        assertEquals(userId, captor.getValue().getId());
        assertEquals(email, captor.getValue().getEmail());
    }
}
