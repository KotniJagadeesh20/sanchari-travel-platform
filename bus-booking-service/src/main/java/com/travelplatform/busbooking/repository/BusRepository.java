package com.travelplatform.busbooking.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.travelplatform.busbooking.entity.Bus;

@Repository
public interface BusRepository extends JpaRepository<Bus, UUID> {

	@Query("SELECT b FROM Bus b WHERE b.busno = ?1")
	Bus findBusByBusNo(String num);

	@Query("SELECT b FROM Bus b")
	List<Bus> findAllBus();

	//@Query("DELETE FROM Bus b WHERE b.busno = :busNo")
    @Transactional
    void deleteByBusno(@Param("busNo") String busNo);

	/**
	 * Case/whitespace-insensitive match on source and destination — a caller
	 * passing " Goa " or "GOA" matches a stored "Goa" the same as an exact
	 * match would. Normalizes both sides with TRIM+LOWER rather than relying
	 * on callers to sanitize input. Backs GET /api/user/searchbusses/{source}/{destination}/{date}.
	 */
	@Query("SELECT b FROM Bus b WHERE LOWER(TRIM(b.source)) = LOWER(TRIM(:source)) " +
			"AND LOWER(TRIM(b.destination)) = LOWER(TRIM(:destination)) " +
			"AND b.date = :date")
	List<Bus> findBySourceAndDestinationAndDate(@Param("source") String source,
			@Param("destination") String destination,
			@Param("date") LocalDate date);
	
	
	
	@Query("SELECT b FROM Bus b WHERE b.id = ?1")
    Bus findByBusId(UUID busId);

}
