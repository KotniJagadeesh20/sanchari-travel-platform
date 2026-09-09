package com.travelplatform.rideshare.repository;

import com.travelplatform.rideshare.entity.RideBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RideBookingRepository extends JpaRepository<RideBooking, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    java.util.Optional<RideBooking> findForUpdateById(UUID id);

    List<RideBooking> findByPassengerId(UUID passengerId);

    List<RideBooking> findByRideId(UUID rideId);
}
