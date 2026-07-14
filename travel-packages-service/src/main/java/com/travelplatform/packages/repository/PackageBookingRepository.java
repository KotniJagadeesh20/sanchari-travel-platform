package com.travelplatform.packages.repository;

import com.travelplatform.packages.entity.PackageBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PackageBookingRepository extends JpaRepository<PackageBooking, UUID> {

    List<PackageBooking> findByTravelerId(UUID travelerId);

    /** Navigates departure -> travelPackage since PackageBooking no longer has a direct travelPackage FK (see departure). */
    List<PackageBooking> findByDeparture_TravelPackage_Id(UUID travelPackageId);
}
