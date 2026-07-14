package com.travelplatform.busbooking.service;

import java.util.UUID;
import java.util.List;

import com.travelplatform.busbooking.entity.Bookingdetails;

public interface BookingDetailsService {
	
	Bookingdetails savebookingdetails(Bookingdetails bookingdetails);
	
	List<Object>  getBookingDetails(UUID userid);
	
	Boolean cancelTickets(UUID id);

}
