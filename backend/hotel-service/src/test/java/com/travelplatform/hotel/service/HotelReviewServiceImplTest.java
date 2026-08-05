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

import com.travelplatform.hotel.dto.CreateReviewRequest;
import com.travelplatform.hotel.entity.Hotel;
import com.travelplatform.hotel.entity.HotelReview;
import com.travelplatform.hotel.exception.DuplicateReviewException;
import com.travelplatform.hotel.repository.HotelRepository;
import com.travelplatform.hotel.repository.HotelReviewRepository;

@ExtendWith(MockitoExtension.class)
class HotelReviewServiceImplTest {

    @Mock private HotelReviewRepository reviewRepo;
    @Mock private HotelRepository hotelRepo;
    @Mock private HotelService hotelService;
    @InjectMocks private HotelReviewServiceImpl reviewService;

    private Hotel hotel;
    private UUID userId;

    @BeforeEach
    void setUp() {
        hotel = new Hotel();
        hotel.setId(UUID.randomUUID());
        hotel.setAverageRating(4.0);
        hotel.setReviewCount(1);
        userId = UUID.randomUUID();
    }

    @Test
    void rollsNewRatingIntoIncrementalAverage() {
        CreateReviewRequest request = new CreateReviewRequest();
        request.setRating(5);
        request.setComment("Great stay");

        when(hotelService.getHotelById(hotel.getId())).thenReturn(hotel);
        when(reviewRepo.existsByHotelIdAndUserId(hotel.getId(), userId)).thenReturn(false);
        when(reviewRepo.save(any(HotelReview.class))).thenAnswer(i -> i.getArgument(0));
        when(hotelRepo.save(any(Hotel.class))).thenAnswer(i -> i.getArgument(0));

        reviewService.createReview(hotel.getId(), userId, request);

        assertEquals(2, hotel.getReviewCount());
        assertEquals(4.5, hotel.getAverageRating(), 0.0001, "(4.0*1 + 5) / 2 = 4.5");
    }

    @Test
    void throwsDuplicateReview_whenUserAlreadyReviewedHotel() {
        CreateReviewRequest request = new CreateReviewRequest();
        request.setRating(3);

        when(hotelService.getHotelById(hotel.getId())).thenReturn(hotel);
        when(reviewRepo.existsByHotelIdAndUserId(hotel.getId(), userId)).thenReturn(true);

        assertThrows(DuplicateReviewException.class,
                () -> reviewService.createReview(hotel.getId(), userId, request));
        verify(reviewRepo, never()).save(any());
    }
}
