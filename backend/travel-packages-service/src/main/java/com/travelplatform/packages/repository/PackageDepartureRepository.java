package com.travelplatform.packages.repository;

import com.travelplatform.packages.entity.PackageDeparture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PackageDepartureRepository extends JpaRepository<PackageDeparture, UUID> {

    List<PackageDeparture> findByTravelPackageId(UUID travelPackageId);
}
