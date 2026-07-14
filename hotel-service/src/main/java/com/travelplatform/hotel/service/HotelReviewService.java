package com.travelplatform.hotel.service;

import com.travelplatform.hotel.dto.CreateReviewRequest;
import com.travelplatform.hotel.entity.HotelReview;

import java.util.List;
import java.util.UUID;

public interface HotelReviewService {

    HotelReview createReview(UUID hotelId, UUID userId, CreateReviewRequest request);

    List<HotelReview> getReviewsByHotel(UUID hotelId);
}
