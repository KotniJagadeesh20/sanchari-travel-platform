package com.travelplatform.hotel.repository;

import com.travelplatform.hotel.entity.HotelImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface HotelImageRepository extends JpaRepository<HotelImage, UUID> {
}
