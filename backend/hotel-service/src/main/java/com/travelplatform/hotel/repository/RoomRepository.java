package com.travelplatform.hotel.repository;

import com.travelplatform.hotel.entity.Room;
import com.travelplatform.hotel.enums.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RoomRepository extends JpaRepository<Room, UUID> {

    List<Room> findByHotelIdAndActiveTrue(UUID hotelId);

    List<Room> findByHotelId(UUID hotelId);

    List<Room> findByHotelIdAndRoomTypeAndActiveTrue(UUID hotelId, RoomType roomType);
}
