package com.travelplatform.hotel.repository;

import com.travelplatform.hotel.entity.Hotel;
import com.travelplatform.hotel.enums.RoomType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface HotelRepository extends JpaRepository<Hotel, UUID> {

    /**
     * checkIn/checkOut/guests are accepted by the controller for API-contract
     * compatibility but are not applied here as a true per-night availability
     * filter — see Room.availableRooms Javadoc. minPrice/maxPrice/roomType filter
     * on whether the hotel has *any* matching room type at all, via a semi-join.
     */
    @Query("""
        SELECT DISTINCT h FROM Hotel h
        WHERE h.active = true
          AND (:destinationId IS NULL OR h.destinationId = :destinationId)
          AND (:starRating IS NULL OR h.starRating = :starRating)
          AND (
                (:roomType IS NULL AND :minPrice IS NULL AND :maxPrice IS NULL)
                OR EXISTS (
                    SELECT r FROM Room r
                    WHERE r.hotel = h
                      AND r.active = true
                      AND (:roomType IS NULL OR r.roomType = :roomType)
                      AND (:minPrice IS NULL OR r.pricePerNight >= :minPrice)
                      AND (:maxPrice IS NULL OR r.pricePerNight <= :maxPrice)
                )
              )
        """)
    Page<Hotel> search(@Param("destinationId") UUID destinationId,
                        @Param("starRating") Integer starRating,
                        @Param("roomType") RoomType roomType,
                        @Param("minPrice") Double minPrice,
                        @Param("maxPrice") Double maxPrice,
                        Pageable pageable);
}
