package com.travelplatform.busbooking.service;

import java.util.UUID;
import java.util.List;

import com.travelplatform.busbooking.entity.Bookingdetails;

public interface BookingDetailsService {
	
	Bookingdetails savebookingdetails(Bookingdetails bookingdetails);
	
	List<Object>  getBookingDetails(UUID userid);
	
	/**
	 * Cancels a booking, but only if it belongs to requestingUserId.
	 * Returns false if no booking with this id exists.
	 * Throws UnauthorizedBookingActionException if the booking exists but belongs to someone else.
	 */
	Boolean cancelTickets(UUID id, UUID requestingUserId);

}
