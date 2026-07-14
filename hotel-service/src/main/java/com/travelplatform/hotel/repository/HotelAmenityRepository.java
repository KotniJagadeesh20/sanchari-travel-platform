package com.travelplatform.hotel.repository;

import com.travelplatform.hotel.entity.HotelAmenity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface HotelAmenityRepository extends JpaRepository<HotelAmenity, UUID> {
}
