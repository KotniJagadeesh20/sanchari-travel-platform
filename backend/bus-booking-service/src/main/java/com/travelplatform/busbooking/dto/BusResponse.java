package com.travelplatform.busbooking.dto;

import java.util.List;

import com.travelplatform.busbooking.entity.Bus;

public class BusResponse {
	
	private String message;

	private boolean success;

	private Bus bus;
	
	private List<Bus> busses;
	
	public BusResponse() {

	}

	public BusResponse(String message, boolean success, Bus bus, List<Bus> busses) {
		super();
		this.message = message;
		this.success = success;
		this.bus = bus;
		this.busses = busses;
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

	public Bus getBus() {
		return bus;
	}

	public void setBus(Bus bus) {
		this.bus = bus;
	}

	public List<Bus> getBusses() {
		return busses;
	}

	public void setBusses(List<Bus> busses) {
		this.busses = busses;
	}
	
	

}
