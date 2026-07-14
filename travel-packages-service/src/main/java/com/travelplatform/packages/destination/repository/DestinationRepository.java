package com.travelplatform.packages.destination.repository;

import com.travelplatform.packages.destination.entity.Destination;
import com.travelplatform.packages.destination.enums.DestinationCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DestinationRepository extends JpaRepository<Destination, UUID> {

    List<Destination> findByActiveTrue();

    List<Destination> findByCategoryAndActiveTrue(DestinationCategory category);

    List<Destination> findByNameContainingIgnoreCaseAndActiveTrue(String keyword);

    /** averageBudget <= the given ceiling, listed destinations only — backs the search?budget= filter. */
    List<Destination> findByAverageBudgetLessThanEqualAndActiveTrue(Double maxBudget);

    /** Listed destinations ordered by rating, for the "popular" endpoint. */
    List<Destination> findByActiveTrueOrderByManualRatingDesc();
}
