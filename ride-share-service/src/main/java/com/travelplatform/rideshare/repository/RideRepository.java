package com.travelplatform.rideshare.repository;

import com.travelplatform.rideshare.entity.Ride;
import com.travelplatform.rideshare.enums.RideStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface RideRepository extends JpaRepository<Ride, UUID> {

    /**
     * Search only rides that are still bookable (excludes COMPLETED/CANCELLED).
     * Case- and surrounding-whitespace-insensitive on source/destination —
     * a plain equals() derived query here made search fail on "hyderabad" vs
     * "Hyderabad" or a stray trailing space, which is unreliable for a
     * marketplace where drivers and passengers type city names freely.
     * Callers should still trim+normalize what they pass in where practical
     * (see RideServiceImpl.searchRides) — this is a safety net, not a
     * substitute for a real destination picker.
     */
    @Query("SELECT r FROM Ride r WHERE " +
            "LOWER(TRIM(r.source)) = LOWER(TRIM(:source)) AND " +
            "LOWER(TRIM(r.destination)) = LOWER(TRIM(:destination)) AND " +
            "r.travelDate = :travelDate AND r.status = :status")
    List<Ride> findBySourceAndDestinationAndTravelDateAndStatus(
            @Param("source") String source, @Param("destination") String destination,
            @Param("travelDate") LocalDate travelDate, @Param("status") RideStatus status);

    List<Ride> findByDriverId(UUID driverId);
}
