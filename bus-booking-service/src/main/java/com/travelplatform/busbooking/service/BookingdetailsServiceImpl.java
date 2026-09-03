package com.travelplatform.busbooking.service;

import java.util.UUID;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.travelplatform.busbooking.client.NotificationClient;
import com.travelplatform.busbooking.entity.Bookingdetails;
import com.travelplatform.busbooking.entity.Driver;
import com.travelplatform.busbooking.repository.BookingDetailsRepository;


@Service
public class BookingdetailsServiceImpl implements BookingDetailsService {
	
	@Autowired
	private BookingDetailsRepository bookRepo;

	@Autowired
	private NotificationClient notificationClient;

	@Override
	public Bookingdetails savebookingdetails(Bookingdetails bookingdetails) {
		
		Bookingdetails booking=bookRepo.save(bookingdetails);

		// Fire-and-forget — see NotificationClientImpl. Never blocks or fails the booking.
		// userId/email come straight off the saved entity, so no controller changes needed.
		if (booking.getUser() != null) {
			notificationClient.notify(
					booking.getUser().getId(), booking.getEmail(), "BOOKING_CONFIRMED",
					"Bus ticket booked",
					String.format("Your bus ticket (booking %s) has been booked. Total: %d.",
							booking.getId(), booking.getPrice()),
					booking.getId());
		}

		return booking;
	}

	@Override
	public List<Object>  getBookingDetails(UUID userid) {
		List<Object> bookingDetails=bookRepo.getBookingDetails(userid);
		if(bookingDetails!=null) {
			return bookingDetails;
		}
		return null;
	}

	@Override
	public Boolean cancelTickets(UUID id, UUID requestingUserId) {
		Bookingdetails ticket= bookRepo.findByid(id);
		if(ticket!=null) {
			  if (ticket.getUser() == null || !ticket.getUser().getId().equals(requestingUserId)) {
			      throw new com.travelplatform.busbooking.exception.UnauthorizedBookingActionException(
			              "You are not authorized to cancel this booking.");
			  }
			  try {
			        bookRepo.deleteByid(id);

			        if (ticket.getUser() != null) {
			        	notificationClient.notify(
			        			ticket.getUser().getId(), ticket.getEmail(), "BOOKING_CANCELLED",
			        			"Bus ticket cancelled",
			        			String.format("Your bus ticket (booking %s) has been cancelled.", ticket.getId()),
			        			ticket.getId());
			        }

			        return true;
			      
			    } catch (Exception e) {
			        e.printStackTrace();
			        return false;
			    }
			
		}else {
			return false;
		}
	}


}
