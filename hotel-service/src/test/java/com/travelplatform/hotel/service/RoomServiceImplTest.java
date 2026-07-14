package com.travelplatform.hotel.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.travelplatform.hotel.dto.UpdateRoomRequest;
import com.travelplatform.hotel.entity.Room;
import com.travelplatform.hotel.repository.RoomAmenityRepository;
import com.travelplatform.hotel.repository.RoomImageRepository;
import com.travelplatform.hotel.repository.RoomRepository;

@ExtendWith(MockitoExtension.class)
class RoomServiceImplTest {

    @Mock private RoomRepository roomRepo;
    @Mock private RoomImageRepository roomImageRepo;
    @Mock private RoomAmenityRepository roomAmenityRepo;
    @Mock private HotelService hotelService;
    @InjectMocks private RoomServiceImpl roomService;

    @Test
    void updateRoom_shiftsAvailableRoomsByTotalRoomsDelta() {
        Room room = new Room();
        room.setId(UUID.randomUUID());
        room.setTotalRooms(10);
        room.setAvailableRooms(3); // 7 currently booked out

        when(roomRepo.findById(room.getId())).thenReturn(Optional.of(room));
        when(roomRepo.save(any(Room.class))).thenAnswer(i -> i.getArgument(0));

        UpdateRoomRequest request = new UpdateRoomRequest();
        request.setTotalRooms(15); // +5 inventory added

        Room updated = roomService.updateRoom(room.getId(), request);

        assertEquals(15, updated.getTotalRooms());
        assertEquals(8, updated.getAvailableRooms(), "availableRooms shifts by the same +5 delta, preserving the 7 booked out");
    }

    @Test
    void updateRoom_flooresAvailableRoomsAtZero_whenInventoryShrinksBelowBookedOut() {
        Room room = new Room();
        room.setId(UUID.randomUUID());
        room.setTotalRooms(10);
        room.setAvailableRooms(2); // 8 booked out

        when(roomRepo.findById(room.getId())).thenReturn(Optional.of(room));
        when(roomRepo.save(any(Room.class))).thenAnswer(i -> i.getArgument(0));

        UpdateRoomRequest request = new UpdateRoomRequest();
        request.setTotalRooms(5); // shrinks below what's already booked out

        Room updated = roomService.updateRoom(room.getId(), request);

        assertEquals(0, updated.getAvailableRooms(), "Never goes negative even if shrink exceeds current availability");
    }

    @Test
    void deleteRoom_softDeletesRatherThanRemovingRow() {
        Room room = new Room();
        room.setId(UUID.randomUUID());
        room.setActive(true);

        when(roomRepo.findById(room.getId())).thenReturn(Optional.of(room));
        when(roomRepo.save(any(Room.class))).thenAnswer(i -> i.getArgument(0));

        roomService.deleteRoom(room.getId());

        assertFalse(room.getActive());
        verify(roomRepo, never()).deleteById(any());
    }
}
