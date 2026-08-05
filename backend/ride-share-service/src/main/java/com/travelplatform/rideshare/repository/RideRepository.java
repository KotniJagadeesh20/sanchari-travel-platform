package com.travelplatform.rideshare.repository;

import com.travelplatform.rideshare.entity.Ride;
import com.travelplatform.rideshare.enums.RideStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface RideRepository extends JpaRepository<Ride, UUID> {

    /** Search only rides that are still bookable (excludes COMPLETED/CANCELLED). */
    List<Ride> findBySourceAndDestinationAndTravelDateAndStatus(
            String source, String destination, LocalDate travelDate, RideStatus status);

    List<Ride> findByDriverId(UUID driverId);
}
