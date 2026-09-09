package com.travelplatform.hotel.repository;

import com.travelplatform.hotel.entity.HotelBooking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
import java.time.LocalDate;
import com.travelplatform.hotel.enums.BookingStatus;

public interface HotelBookingRepository extends JpaRepository<HotelBooking, UUID> {

    List<HotelBooking> findByUserIdOrderByBookingDateDesc(UUID userId);

    List<HotelBooking> findByHotelIdOrderByBookingDateDesc(UUID hotelId);

    long countByRoomIdAndBookingStatusNotAndCheckInDateLessThanAndCheckOutDateGreaterThan(
            UUID roomId, BookingStatus excludedStatus, LocalDate checkOut, LocalDate checkIn);
}
