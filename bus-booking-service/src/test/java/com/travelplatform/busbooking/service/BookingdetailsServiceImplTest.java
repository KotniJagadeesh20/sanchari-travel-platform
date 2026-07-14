package com.travelplatform.busbooking.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.travelplatform.busbooking.client.NotificationClient;
import com.travelplatform.busbooking.entity.Bookingdetails;
import com.travelplatform.busbooking.repository.BookingDetailsRepository;

@ExtendWith(MockitoExtension.class)
class BookingdetailsServiceImplTest {

	@Mock
	private BookingDetailsRepository bookRepo;

	@Mock
	private NotificationClient notificationClient;

	@InjectMocks
	private BookingdetailsServiceImpl bookingService;

	private Bookingdetails booking;

	@BeforeEach
	void setUp() {
		booking = new Bookingdetails();
		booking.setId(UUID.randomUUID());
		booking.setName("Asha");
		booking.setEmail("asha@example.com");
		booking.setPhoneno("9876543210");
		booking.setAge(28);
		booking.setPrice(800);
	}

	@Test
	void savebookingdetails_returnsSavedBooking() {
		when(bookRepo.save(booking)).thenReturn(booking);

		Bookingdetails result = bookingService.savebookingdetails(booking);

		assertEquals(booking, result);
		verify(bookRepo).save(booking);
	}

	@Test
	void getBookingDetails_returnsListFromRepo() {
		List<Object> rows = Arrays.asList(new Object[] { "Asha", "asha@example.com" });
		UUID userId = UUID.randomUUID();
		when(bookRepo.getBookingDetails(userId)).thenReturn(rows);

		List<Object> result = bookingService.getBookingDetails(userId);

		assertEquals(rows, result);
	}

	@Test
	void cancelTickets_returnsTrue_whenBookingExists() {
		when(bookRepo.findByid(booking.getId())).thenReturn(booking);

		boolean result = bookingService.cancelTickets(booking.getId());

		assertTrue(result);
		verify(bookRepo).deleteByid(booking.getId());
	}

	@Test
	void cancelTickets_returnsFalse_whenBookingNotFound() {
		UUID missingId = UUID.randomUUID();
		when(bookRepo.findByid(missingId)).thenReturn(null);

		boolean result = bookingService.cancelTickets(missingId);

		assertFalse(result);
		verify(bookRepo, never()).deleteByid(any());
	}
}
