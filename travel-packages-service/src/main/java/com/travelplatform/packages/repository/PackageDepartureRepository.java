package com.travelplatform.packages.repository;

import com.travelplatform.packages.entity.PackageDeparture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.UUID;

@Repository
public interface PackageDepartureRepository extends JpaRepository<PackageDeparture, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    java.util.Optional<PackageDeparture> findForUpdateById(UUID id);

    List<PackageDeparture> findByTravelPackageId(UUID travelPackageId);
}
