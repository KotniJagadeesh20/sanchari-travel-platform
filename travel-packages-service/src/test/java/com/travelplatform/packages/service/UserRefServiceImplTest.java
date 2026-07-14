package com.travelplatform.packages.service;

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

import com.travelplatform.packages.entity.UserRef;
import com.travelplatform.packages.repository.UserRefRepository;

@ExtendWith(MockitoExtension.class)
class UserRefServiceImplTest {

    @Mock private UserRefRepository userRefRepo;
    @InjectMocks private UserRefServiceImpl userRefService;

    private UUID userId;
    private String email;
    private String name;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        email = "asha@example.com";
        name = "Asha Rao";
    }

    @Test
    void findOrCreate_returnsExisting_whenAlreadyPresent() {
        UserRef existing = new UserRef(userId, email, name);
        when(userRefRepo.findById(userId)).thenReturn(Optional.of(existing));

        UserRef result = userRefService.findOrCreate(userId, email, name);

        assertEquals(existing, result);
        verify(userRefRepo, never()).save(any());
    }

    @Test
    void findOrCreate_createsAndSaves_whenNotPresent() {
        when(userRefRepo.findById(userId)).thenReturn(Optional.empty());
        when(userRefRepo.save(any(UserRef.class))).thenAnswer(i -> i.getArgument(0));

        UserRef result = userRefService.findOrCreate(userId, email, name);

        assertEquals(userId, result.getId());
        assertEquals(email, result.getEmail());
        assertEquals(name, result.getName());
        verify(userRefRepo).save(any(UserRef.class));
    }

    @Test
    void findOrCreate_savesCorrectValues() {
        when(userRefRepo.findById(userId)).thenReturn(Optional.empty());
        when(userRefRepo.save(any(UserRef.class))).thenAnswer(i -> i.getArgument(0));

        userRefService.findOrCreate(userId, email, name);

        ArgumentCaptor<UserRef> captor = ArgumentCaptor.forClass(UserRef.class);
        verify(userRefRepo).save(captor.capture());
        assertEquals(userId, captor.getValue().getId());
        assertEquals(email, captor.getValue().getEmail());
        assertEquals(name, captor.getValue().getName());
    }

    @Test
    void findOrCreate_syncsStaleNameAndEmail_whenChanged() {
        UserRef existing = new UserRef(userId, "old@example.com", "Old Name");
        when(userRefRepo.findById(userId)).thenReturn(Optional.of(existing));
        when(userRefRepo.save(any(UserRef.class))).thenAnswer(i -> i.getArgument(0));

        UserRef result = userRefService.findOrCreate(userId, email, name);

        assertEquals(email, result.getEmail());
        assertEquals(name, result.getName());
        verify(userRefRepo).save(existing);
    }
}
