package com.travelplatform.hotel.repository;

import com.travelplatform.hotel.entity.HotelBooking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HotelBookingRepository extends JpaRepository<HotelBooking, UUID> {

    List<HotelBooking> findByUserIdOrderByBookingDateDesc(UUID userId);

    List<HotelBooking> findByHotelIdOrderByBookingDateDesc(UUID hotelId);
}
