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

	//@Query("SELECT b FROM Bus b WHERE b.source = ?1 AND b.destination = ?2 AND b.date = ?3")
	List<Bus> findBySourceAndDestinationAndDate(String source, String destination, LocalDate date);
	
	
	
	@Query("SELECT b FROM Bus b WHERE b.id = ?1")
    Bus findByBusId(UUID busId);

}
