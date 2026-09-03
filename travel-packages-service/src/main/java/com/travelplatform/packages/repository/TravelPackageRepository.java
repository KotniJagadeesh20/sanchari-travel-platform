package com.travelplatform.packages.repository;

import com.travelplatform.packages.entity.TravelPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Combined search — every provided filter is applied together (AND), not
     * just whichever one happens to be present. Pass null for any filter you
     * don't want applied. Mirrors DestinationRepository.search. Backs
     * GET /packages/search.
     */
    @Query("SELECT p FROM TravelPackage p WHERE p.active = true " +
            "AND (:destinationId IS NULL OR p.destinationId = :destinationId) " +
            "AND (:keyword IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "     OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:maxBudget IS NULL OR p.price <= :maxBudget) " +
            "AND (:minDurationDays IS NULL OR p.durationDays >= :minDurationDays) " +
            "AND (:maxDurationDays IS NULL OR p.durationDays <= :maxDurationDays)")
    List<TravelPackage> search(@Param("destinationId") UUID destinationId,
                                @Param("keyword") String keyword,
                                @Param("maxBudget") Double maxBudget,
                                @Param("minDurationDays") Integer minDurationDays,
                                @Param("maxDurationDays") Integer maxDurationDays);
}
