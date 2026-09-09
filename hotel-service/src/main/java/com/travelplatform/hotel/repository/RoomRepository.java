package com.travelplatform.hotel.repository;

import com.travelplatform.hotel.entity.Room;
import com.travelplatform.hotel.enums.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.UUID;

public interface RoomRepository extends JpaRepository<Room, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    java.util.Optional<Room> findForUpdateById(UUID id);

    List<Room> findByHotelIdAndActiveTrue(UUID hotelId);

    List<Room> findByHotelId(UUID hotelId);

    List<Room> findByHotelIdAndRoomTypeAndActiveTrue(UUID hotelId, RoomType roomType);
}
