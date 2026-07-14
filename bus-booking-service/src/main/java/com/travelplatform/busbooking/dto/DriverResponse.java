package com.travelplatform.busbooking.dto;

import java.util.List;

import com.travelplatform.busbooking.entity.Driver;

public class DriverResponse {
	
	private String message;

	private boolean success;

	private Driver driver;
	
	private List<Driver> drivers;
	
	public DriverResponse() {

	}

	

	public DriverResponse(String message, boolean success, Driver driver, List<Driver> drivers) {
		super();
		this.message = message;
		this.success = success;
		this.driver = driver;
		this.drivers = drivers;
	}



	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public Driver getDriver() {
		return driver;
	}

	public void setDriver(Driver driver) {
		this.driver = driver;
	}



	public List<Driver> getDrivers() {
		return drivers;
	}



	public void setDrivers(List<Driver> drivers) {
		this.drivers = drivers;
	}
	
	

	

	

	

	
	
	
	

}
