package com.travelplatform.hotel.repository;

import com.travelplatform.hotel.entity.Room;
import com.travelplatform.hotel.enums.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RoomRepository extends JpaRepository<Room, UUID> {

    List<Room> findByHotelIdAndActiveTrue(UUID hotelId);

    List<Room> findByHotelId(UUID hotelId);

    List<Room> findByHotelIdAndRoomTypeAndActiveTrue(UUID hotelId, RoomType roomType);

    /**
     * Atomic, concurrency-safe reservation — same pattern as
     * PackageDepartureRepository.decrementAvailableSlots(). The WHERE clause
     * and the decrement happen as one UPDATE statement, so two concurrent
     * bookings against the same room can't both read the same
     * availableRooms and both succeed. Returns rows updated: 1 on success,
     * 0 if there was no inventory left (or the id doesn't exist).
     *
     * Note: this alone does not make availability date-aware (see
     * Room.availableRooms Javadoc) — it only closes the concurrency race on
     * the existing single-counter model.
     */
    @Modifying
    @Query("UPDATE Room r SET r.availableRooms = r.availableRooms - 1 " +
            "WHERE r.id = :id AND r.availableRooms >= 1")
    int decrementAvailableRooms(@Param("id") UUID id);

    /** Atomic release of a previously-reserved room (cancellation). */
    @Modifying
    @Query("UPDATE Room r SET r.availableRooms = r.availableRooms + 1 WHERE r.id = :id")
    int incrementAvailableRooms(@Param("id") UUID id);
}
