package com.travelplatform.busbooking.service;

import java.util.UUID;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.travelplatform.busbooking.entity.Bus;

import com.travelplatform.busbooking.repository.BusRepository;

@Service
public class BusServiceImpl implements BusService {
	
	@Autowired
	private BusRepository busRepo;

	@Override
	public Bus addBus(Bus addBus) {
		
		Bus isBus=busRepo.findBusByBusNo(addBus.getBusno());
		if(isBus!=null) {
			return null;
		}else {
			return busRepo.save(addBus);
		}	
		
	}

	@Override
	public List<Bus> getAllBusses() {
		List<Bus> allBusses=busRepo.findAllBus();
		return allBusses;
	}

	@Override
	public boolean deletebus(String busNo) {
		busRepo.deleteByBusno(busNo);
		Bus deletebus=busRepo.findBusByBusNo(busNo);
		if(deletebus==null) {
			return true;
		}
		return  false;
	}

	@Override
	public List<Bus> Searchbus(String source, String destination, LocalDate date) {
		List<Bus> searchBusses=busRepo.findBySourceAndDestinationAndDate(source, destination, date);
		return searchBusses;
	}

	@Override
	public Bus finderBusbyId(UUID id) {
		Bus bus=busRepo.findByBusId(id);
		return bus;
	}

	@Override
	public Bus Editbus(Bus bus) {
		Bus isBus=busRepo.findBusByBusNo(bus.getBusno());
		if(isBus!=null) {
			// Copy incoming fields onto the entity we already fetched (which
			// carries the correct existing id) rather than saving the raw
			// request body directly. Saving `bus` as-is would leave its `id`
			// null (the client never sends one), and Hibernate treats a
			// null-id save as a new row to INSERT — which then fails on
			// busno's unique constraint instead of updating the existing row.
			isBus.setSource(bus.getSource());
			isBus.setDestination(bus.getDestination());
			isBus.setBusType(bus.getBusType());
			isBus.setDate(bus.getDate());
			isBus.setTime(bus.getTime());
			isBus.setArrivalDate(bus.getArrivalDate());
			isBus.setArrivalTime(bus.getArrivalTime());
			isBus.setPrice(bus.getPrice());
			return busRepo.save(isBus);
		}else {
			return null;
		}
	
	}

	
	
	

}
