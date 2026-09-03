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

import com.travelplatform.hotel.dto.CreateHotelRequest;
import com.travelplatform.hotel.dto.UpdateHotelRequest;
import com.travelplatform.hotel.entity.Hotel;
import com.travelplatform.hotel.entity.UserRef;
import com.travelplatform.hotel.exception.HotelNotFoundException;
import com.travelplatform.hotel.repository.HotelAmenityRepository;
import com.travelplatform.hotel.repository.HotelImageRepository;
import com.travelplatform.hotel.repository.HotelRepository;

import java.util.Arrays;
import java.util.List;

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

    @Test
    void createHotel_setsCreatedBy() {
        CreateHotelRequest request = new CreateHotelRequest();
        request.setName("Test Hotel");
        request.setDestinationId(UUID.randomUUID());
        request.setAddress("123 Main St");
        request.setCity("Hyderabad");
        request.setCountry("India");
        request.setStarRating(4);

        UserRef creator = new UserRef(UUID.randomUUID(), "owner@example.com", "Owner");
        when(hotelRepo.save(any(Hotel.class))).thenAnswer(i -> i.getArgument(0));

        Hotel created = hotelService.createHotel(request, creator);

        assertEquals(creator, created.getCreatedBy());
    }

    @Test
    void getAllHotelsForAdmin_returnsEveryHotelRegardlessOfActiveFlag() {
        Hotel active = new Hotel();
        Hotel delisted = new Hotel();
        when(hotelRepo.findAll()).thenReturn(Arrays.asList(active, delisted));

        List<Hotel> result = hotelService.getAllHotelsForAdmin();

        assertEquals(2, result.size());
    }

    @Test
    void getHotelsByCreator_delegatesToRepositoryQuery() {
        UUID creatorId = UUID.randomUUID();
        Hotel hotel = new Hotel();
        when(hotelRepo.findByCreatedById(creatorId)).thenReturn(List.of(hotel));

        List<Hotel> result = hotelService.getHotelsByCreator(creatorId);

        assertEquals(1, result.size());
        verify(hotelRepo).findByCreatedById(creatorId);
    }
}
