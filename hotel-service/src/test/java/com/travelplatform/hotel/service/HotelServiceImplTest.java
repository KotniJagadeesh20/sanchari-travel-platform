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

import com.travelplatform.hotel.dto.UpdateHotelRequest;
import com.travelplatform.hotel.entity.Hotel;
import com.travelplatform.hotel.exception.HotelNotFoundException;
import com.travelplatform.hotel.repository.HotelAmenityRepository;
import com.travelplatform.hotel.repository.HotelImageRepository;
import com.travelplatform.hotel.repository.HotelRepository;

@ExtendWith(MockitoExtension.class)
class HotelServiceImplTest {

    @Mock private HotelRepository hotelRepo;
    @Mock private HotelImageRepository hotelImageRepo;
    @Mock private HotelAmenityRepository hotelAmenityRepo;
    @InjectMocks private HotelServiceImpl hotelService;

    @Test
    void deleteHotel_softDeletesRatherThanRemovingRow() {
        Hotel hotel = new Hotel();
        hotel.setId(UUID.randomUUID());
        hotel.setActive(true);

        when(hotelRepo.findById(hotel.getId())).thenReturn(Optional.of(hotel));
        when(hotelRepo.save(any(Hotel.class))).thenAnswer(i -> i.getArgument(0));

        hotelService.deleteHotel(hotel.getId());

        assertFalse(hotel.getActive(), "delete should delist (active=false), not hard-delete");
        verify(hotelRepo, never()).deleteById(any());
        verify(hotelRepo).save(hotel);
    }

    @Test
    void getHotelById_throwsHotelNotFound_whenMissing() {
        UUID missingId = UUID.randomUUID();
        when(hotelRepo.findById(missingId)).thenReturn(Optional.empty());

        assertThrows(HotelNotFoundException.class, () -> hotelService.getHotelById(missingId));
    }

    @Test
    void updateHotel_onlyAppliesNonNullFields() {
        Hotel hotel = new Hotel();
        hotel.setId(UUID.randomUUID());
        hotel.setName("Old Name");
        hotel.setCity("Old City");

        when(hotelRepo.findById(hotel.getId())).thenReturn(Optional.of(hotel));
        when(hotelRepo.save(any(Hotel.class))).thenAnswer(i -> i.getArgument(0));

        UpdateHotelRequest request = new UpdateHotelRequest();
        request.setName("New Name"); // city left null — should stay unchanged

        Hotel updated = hotelService.updateHotel(hotel.getId(), request);

        assertEquals("New Name", updated.getName());
        assertEquals("Old City", updated.getCity(), "Null fields on the request must not overwrite existing values");
    }
}
