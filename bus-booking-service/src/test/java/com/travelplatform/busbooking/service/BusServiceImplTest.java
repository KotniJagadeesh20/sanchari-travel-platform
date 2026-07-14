package com.travelplatform.busbooking.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.travelplatform.busbooking.entity.Bus;
import com.travelplatform.busbooking.repository.BusRepository;

@ExtendWith(MockitoExtension.class)
class BusServiceImplTest {

	@Mock
	private BusRepository busRepo;

	@InjectMocks
	private BusServiceImpl busService;

	private Bus bus;

	@BeforeEach
	void setUp() {
		bus = new Bus();
		bus.setId(UUID.randomUUID());
		bus.setBusno("AP01");
		bus.setSource("Hyderabad");
		bus.setDestination("Bangalore");
		bus.setBusType("AC Sleeper");
		bus.setDate(LocalDate.of(2026, 6, 20));
		bus.setPrice(800);
	}

	@Test
	void addBus_savesNewBus_whenBusNoNotPresent() {
		when(busRepo.findBusByBusNo("AP01")).thenReturn(null);
		when(busRepo.save(bus)).thenReturn(bus);

		Bus saved = busService.addBus(bus);

		assertNotNull(saved);
		assertEquals("AP01", saved.getBusno());
		verify(busRepo).save(bus);
	}

	@Test
	void addBus_returnsNull_whenBusNoAlreadyExists() {
		when(busRepo.findBusByBusNo("AP01")).thenReturn(bus);

		Bus result = busService.addBus(bus);

		assertNull(result);
		verify(busRepo, never()).save(any());
	}

	@Test
	void editBus_updatesExistingBus() {
		when(busRepo.findBusByBusNo("AP01")).thenReturn(bus);
		when(busRepo.save(bus)).thenReturn(bus);

		Bus result = busService.Editbus(bus);

		assertNotNull(result);
		verify(busRepo).save(bus);
	}

	@Test
	void editBus_returnsNull_whenBusDoesNotExist() {
		when(busRepo.findBusByBusNo("AP01")).thenReturn(null);

		Bus result = busService.Editbus(bus);

		assertNull(result);
		verify(busRepo, never()).save(any());
	}

	@Test
	void deletebus_returnsTrue_whenBusNoLongerFound() {
		when(busRepo.findBusByBusNo("AP01")).thenReturn(null);

		boolean deleted = busService.deletebus("AP01");

		assertTrue(deleted);
		verify(busRepo).deleteByBusno("AP01");
	}

	@Test
	void deletebus_returnsFalse_whenBusStillFoundAfterDelete() {
		when(busRepo.findBusByBusNo("AP01")).thenReturn(bus);

		boolean deleted = busService.deletebus("AP01");

		assertFalse(deleted);
	}

	@Test
	void getAllBusses_returnsListFromRepo() {
		when(busRepo.findAllBus()).thenReturn(Arrays.asList(bus));

		List<Bus> result = busService.getAllBusses();

		assertEquals(1, result.size());
		assertEquals("Hyderabad", result.get(0).getSource());
	}

	@Test
	void searchbus_delegatesToRepository() {
		LocalDate date = LocalDate.of(2026, 6, 20);
		when(busRepo.findBySourceAndDestinationAndDate("Hyderabad", "Bangalore", date))
				.thenReturn(Arrays.asList(bus));

		List<Bus> result = busService.Searchbus("Hyderabad", "Bangalore", date);

		assertEquals(1, result.size());
		verify(busRepo).findBySourceAndDestinationAndDate("Hyderabad", "Bangalore", date);
	}

	@Test
	void finderBusbyId_returnsBus() {
		when(busRepo.findByBusId(bus.getId())).thenReturn(bus);

		Bus result = busService.finderBusbyId(bus.getId());

		assertEquals(bus, result);
	}
}
