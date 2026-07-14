package com.travelplatform.busbooking.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.travelplatform.busbooking.entity.Bookingdetails;

@Repository
public interface BookingDetailsRepository extends JpaRepository<Bookingdetails, UUID> {

	@Query("SELECT bd.name, bd.email, bd.phoneno, bd.age, b.source, b.destination " + "FROM Bookingdetails bd "
			+ "INNER JOIN Bus b ON bd.bus.id = b.id " + 
			"INNER JOIN UserAdmin u ON bd.user.id = u.id " + // Assuming																							// user table
			"WHERE u.id = ?1")
	List<Object> getBookingDetails(UUID userid);

	// @Query("SELECT bd.name, bd.email, bd.phoneno, bd.age, bd.bus.source,
	// bd.bus.destination " +
	// "FROM Bookingdetails bd " +
	// "INNER JOIN bd.bus " +
	// "INNER JOIN bd.user u " + // Assuming there's a user attribute in
	// Bookingdetails entity
	// "WHERE u.id = ?1")
	// List<Object> getBookingDetails(int userId);

	Bookingdetails findByid(UUID id);

	@Transactional
	void deleteByid(UUID id);

}
