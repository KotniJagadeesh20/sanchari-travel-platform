package com.travelplatform.packages.repository;

import com.travelplatform.packages.entity.PackageDeparture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PackageDepartureRepository extends JpaRepository<PackageDeparture, UUID> {

    List<PackageDeparture> findByTravelPackageId(UUID travelPackageId);

    /**
     * Atomic, concurrency-safe reservation. Decrements availableSlots only if
     * the row still has at least `count` slots at the moment of the UPDATE —
     * the WHERE clause and the decrement happen as one statement, so two
     * concurrent bookings can't both read the same availableSlots and both
     * succeed (the read-check-write race this replaces). Returns the number
     * of rows updated: 1 on success, 0 if there wasn't enough availability
     * (someone else got there first, or the id doesn't exist) — callers must
     * check this return value rather than assuming success.
     */
    @Modifying
    @Query("UPDATE PackageDeparture pd SET pd.availableSlots = pd.availableSlots - :count " +
            "WHERE pd.id = :id AND pd.availableSlots >= :count")
    int decrementAvailableSlots(@Param("id") UUID id, @Param("count") int count);

    /**
     * Atomic release of previously-reserved slots (cancellation). No lower
     * bound needed here — releasing can't overshoot the entity's own max.
     */
    @Modifying
    @Query("UPDATE PackageDeparture pd SET pd.availableSlots = pd.availableSlots + :count WHERE pd.id = :id")
    int incrementAvailableSlots(@Param("id") UUID id, @Param("count") int count);
}
