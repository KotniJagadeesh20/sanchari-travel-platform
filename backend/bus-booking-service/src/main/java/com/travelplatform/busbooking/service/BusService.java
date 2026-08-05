package com.travelplatform.busbooking.service;

import java.util.UUID;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.travelplatform.busbooking.entity.Bus;

public interface BusService {

	Bus addBus(Bus addBus);
	
	List<Bus> getAllBusses();
	
	boolean deletebus(String busNo);

	List<Bus> Searchbus(String source, String destination, LocalDate date);
	
	Bus finderBusbyId(UUID id);
	
	Bus Editbus(Bus bus);
	

}
