package com.travelplatform.busbooking.service;

import java.util.UUID;
import java.util.List;
import java.util.Optional;

import com.travelplatform.busbooking.entity.Driver;

public interface DriverService {
	
	Driver addDriver(Driver addDriver);
	
	List<Driver> getAllDrivers();
	
	boolean deleteDriver(UUID driverid);
	
	boolean Assigndriver(Driver driver);
	
    Driver findDriverbyid(UUID id);

}
