package com.travelplatform.hotel.repository;

import com.travelplatform.hotel.entity.RoomImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RoomImageRepository extends JpaRepository<RoomImage, UUID> {
}
