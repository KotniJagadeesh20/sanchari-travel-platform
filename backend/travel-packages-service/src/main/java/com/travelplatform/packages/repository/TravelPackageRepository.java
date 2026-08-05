package com.travelplatform.packages.repository;

import com.travelplatform.packages.entity.TravelPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TravelPackageRepository extends JpaRepository<TravelPackage, UUID> {

    /** Browse — only currently listed packages. */
    List<TravelPackage> findByActiveTrue();

    /** Find packages linked to a specific destination UUID, listed only. */
    List<TravelPackage> findByDestinationIdAndActiveTrue(UUID destinationId);

    /** Packages a specific user created (includes delisted ones — this is an admin-facing view). */
    List<TravelPackage> findByCreatedById(UUID createdById);

    /** Admin view — all packages including delisted ones. */
    @Override
    List<TravelPackage> findAll();
}
