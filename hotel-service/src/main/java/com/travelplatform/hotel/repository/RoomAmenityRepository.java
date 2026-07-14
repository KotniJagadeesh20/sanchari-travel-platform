package com.travelplatform.hotel.repository;

import com.travelplatform.hotel.entity.RoomAmenity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RoomAmenityRepository extends JpaRepository<RoomAmenity, UUID> {
}
