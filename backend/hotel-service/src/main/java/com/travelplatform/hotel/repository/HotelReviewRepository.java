package com.travelplatform.hotel.repository;

import com.travelplatform.hotel.entity.HotelReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HotelReviewRepository extends JpaRepository<HotelReview, UUID> {

    List<HotelReview> findByHotelIdOrderByCreatedAtDesc(UUID hotelId);

    boolean existsByHotelIdAndUserId(UUID hotelId, UUID userId);
}
