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

import com.travelplatform.busbooking.entity.Driver;
import com.travelplatform.busbooking.repository.DriverRepository;

@ExtendWith(MockitoExtension.class)
class DriverServiceImplTest {

	@Mock
	private DriverRepository driverRepo;

	@InjectMocks
	private DriverServiceImpl driverService;

	private Driver driver;

	@BeforeEach
	void setUp() {
		driver = new Driver();
		driver.setId(UUID.randomUUID());
		driver.setName("Ravi");
		driver.setEmail("ravi@example.com");
		driver.setAge(35);
		driver.setPhone(9876543210L);
	}

	@Test
	void addDriver_savesAndReturnsDriver() {
		when(driverRepo.save(driver)).thenReturn(driver);

		Driver result = driverService.addDriver(driver);

		assertEquals(driver, result);
		verify(driverRepo).save(driver);
	}

	@Test
	void getAllDrivers_returnsListFromRepo() {
		when(driverRepo.findAll()).thenReturn(Arrays.asList(driver));

		List<Driver> result = driverService.getAllDrivers();

		assertEquals(1, result.size());
		assertEquals("Ravi", result.get(0).getName());
	}

	@Test
	void deleteDriver_returnsTrue_whenDriverExists() {
		when(driverRepo.findByid(driver.getId())).thenReturn(driver);

		boolean result = driverService.deleteDriver(driver.getId());

		assertTrue(result);
		verify(driverRepo).deleteById(driver.getId());
	}

	@Test
	void deleteDriver_returnsFalse_whenDriverNotFound() {
		UUID missingId = UUID.randomUUID();
		when(driverRepo.findByid(missingId)).thenReturn(null);

		boolean result = driverService.deleteDriver(missingId);

		assertFalse(result);
		verify(driverRepo, never()).deleteById(any());
	}

	@Test
	void findDriverbyid_returnsDriverFromRepo() {
		when(driverRepo.findByDriverId(driver.getId())).thenReturn(driver);

		Driver result = driverService.findDriverbyid(driver.getId());

		assertEquals(driver, result);
	}
}
