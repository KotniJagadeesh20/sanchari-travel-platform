package com.travelplatform.packages.destination.repository;

import com.travelplatform.packages.destination.entity.Destination;
import com.travelplatform.packages.destination.enums.DestinationCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DestinationRepository extends JpaRepository<Destination, UUID> {

    List<Destination> findByActiveTrue();

    /**
     * Combined search — every provided filter is applied together (AND), not
     * just whichever one happens to be present. Pass null for any filter you
     * don't want applied. Backs GET /destinations/search.
     */
    @Query("SELECT d FROM Destination d WHERE d.active = true " +
            "AND (:keyword IS NULL OR LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:category IS NULL OR d.category = :category) " +
            "AND (:maxBudget IS NULL OR d.averageBudget <= :maxBudget) " +
            "AND (:visitMonth IS NULL OR :visitMonth MEMBER OF d.bestMonths)")
    List<Destination> search(@Param("keyword") String keyword,
                              @Param("category") DestinationCategory category,
                              @Param("maxBudget") Double maxBudget,
                              @Param("visitMonth") Integer visitMonth);

    /** Listed destinations ordered by rating, for the "popular" endpoint. */
    List<Destination> findByActiveTrueOrderByManualRatingDesc();
}
