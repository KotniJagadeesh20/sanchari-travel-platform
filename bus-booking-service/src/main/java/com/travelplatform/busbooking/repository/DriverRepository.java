package com.travelplatform.busbooking.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.travelplatform.busbooking.entity.Driver;

@Repository
public interface DriverRepository extends JpaRepository<Driver, UUID> {
    
    @Query("SELECT d FROM Driver d WHERE d.id = ?1")
    Driver findByDriverId(UUID driverId);
    
    @Query("SELECT d FROM Driver d")
    List<Driver> findAllDrivers();
	
	Driver findByid(UUID id);
	

	//@Query("DELETE FROM Driver d WHERE d.id = ?1")
	
	@Query("SELECT d FROM Driver d WHERE d.email = ?1")
	Driver findByDrivermail(String mail);

	
}