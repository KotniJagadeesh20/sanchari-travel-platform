package com.travelplatform.busbooking.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.travelplatform.busbooking.dto.BusResponse;
import com.travelplatform.busbooking.dto.DriverResponse;
import com.travelplatform.busbooking.entity.Bus;
import com.travelplatform.busbooking.entity.Driver;
import com.travelplatform.busbooking.repository.BusRepository;
import com.travelplatform.busbooking.repository.DriverRepository;
import com.travelplatform.busbooking.service.BusService;
import com.travelplatform.busbooking.service.DriverService;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@Tag(name = "Admin", description = "Bus and driver management — ROLE_ADMIN only")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    @Autowired private BusService busService;
    @Autowired private DriverService driverService;
    @Autowired private BusRepository busRepo;
    @Autowired private DriverRepository driverRepo;

    // ─── BUS ────────────────────────────────────────────────────────────────

    @Operation(summary = "Add a bus")
    @ApiResponses({ @ApiResponse(responseCode = "202", description = "Bus added"),
                    @ApiResponse(responseCode = "400", description = "Bus number already exists") })
    @PostMapping("/addbus")
    public ResponseEntity<BusResponse> addBus(@RequestBody Bus addBus) {
        if (busRepo.findBusByBusNo(addBus.getBusno()) != null) {
            BusResponse r = new BusResponse();
            r.setSuccess(false);
            r.setMessage("Bus number " + addBus.getBusno() + " already exists");
            return new ResponseEntity<>(r, HttpStatus.BAD_REQUEST);
        }
        Bus saved = busService.addBus(addBus);
        BusResponse r = new BusResponse();
        r.setSuccess(saved != null);
        r.setMessage(saved != null ? "Bus added successfully" : "Failed to add bus");
        return new ResponseEntity<>(r, saved != null ? HttpStatus.ACCEPTED : HttpStatus.BAD_REQUEST);
    }

    @Operation(summary = "Edit a bus")
    @PostMapping("/editBus")
    public ResponseEntity<BusResponse> editBus(@RequestBody Bus addBus) {
        if (busRepo.findBusByBusNo(addBus.getBusno()) == null) {
            BusResponse r = new BusResponse();
            r.setSuccess(false);
            r.setMessage("Bus " + addBus.getBusno() + " not found");
            return new ResponseEntity<>(r, HttpStatus.NOT_FOUND);
        }
        Bus updated = busService.Editbus(addBus);
        BusResponse r = new BusResponse();
        r.setSuccess(updated != null);
        r.setMessage(updated != null ? "Bus updated" : "Update failed");
        return new ResponseEntity<>(r, updated != null ? HttpStatus.ACCEPTED : HttpStatus.BAD_REQUEST);
    }

    @Operation(summary = "Delete a bus by bus number")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Deleted"),
                    @ApiResponse(responseCode = "404", description = "Not found") })
    @DeleteMapping("/deletebus/{busNo}")
    public ResponseEntity<BusResponse> deleteBus(
            @Parameter(description = "Bus number e.g. AP01") @PathVariable String busNo) {
        boolean deleted = busService.deletebus(busNo);
        BusResponse r = new BusResponse();
        r.setSuccess(deleted);
        r.setMessage(deleted ? "Bus " + busNo + " deleted" : "Bus " + busNo + " not found");
        return new ResponseEntity<>(r, deleted ? HttpStatus.OK : HttpStatus.NOT_FOUND);
    }

    @Operation(summary = "List all buses")
    @GetMapping("/allbusses")
    public ResponseEntity<BusResponse> getAllBusses() {
        BusResponse r = new BusResponse();
        r.setBusses(busService.getAllBusses());
        r.setSuccess(true);
        return ResponseEntity.ok(r);
    }

    // ─── DRIVER ─────────────────────────────────────────────────────────────

    @Operation(summary = "Add a driver")
    @PostMapping("/addDriver")
    public ResponseEntity<DriverResponse> addDriver(@RequestBody Driver driver) {
        if (driverRepo.findByDrivermail(driver.getEmail()) != null) {
            DriverResponse r = new DriverResponse();
            r.setSuccess(false);
            r.setMessage("Driver email already registered");
            return new ResponseEntity<>(r, HttpStatus.BAD_REQUEST);
        }
        Driver saved = driverService.addDriver(driver);
        DriverResponse r = new DriverResponse();
        r.setSuccess(saved != null);
        r.setMessage(saved != null ? "Driver added" : "Failed to add driver");
        return new ResponseEntity<>(r, saved != null ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST);
    }

    @Operation(summary = "Edit a driver")
    @PostMapping("/editDriver")
    public ResponseEntity<DriverResponse> editDriver(@RequestBody Driver driver) {
        if (driverRepo.findByDriverId(driver.getId()) == null) {
            DriverResponse r = new DriverResponse();
            r.setSuccess(false);
            r.setMessage("Driver not found");
            return new ResponseEntity<>(r, HttpStatus.NOT_FOUND);
        }
        Driver updated = driverService.addDriver(driver);
        DriverResponse r = new DriverResponse();
        r.setSuccess(updated != null);
        r.setMessage(updated != null ? "Driver updated" : "Update failed");
        return new ResponseEntity<>(r, updated != null ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Operation(summary = "Delete a driver by UUID")
    @DeleteMapping("/deletedriver/{id}")
    public ResponseEntity<DriverResponse> deleteDriver(
            @Parameter(description = "Driver UUID") @PathVariable UUID id) {
        boolean deleted = driverService.deleteDriver(id);
        DriverResponse r = new DriverResponse();
        r.setSuccess(deleted);
        r.setMessage(deleted ? "Driver deleted" : "Driver not found");
        return new ResponseEntity<>(r, deleted ? HttpStatus.OK : HttpStatus.NOT_FOUND);
    }

    @Operation(summary = "List all drivers")
    @GetMapping("/alldrivers")
    public ResponseEntity<DriverResponse> getAllDrivers() {
        DriverResponse r = new DriverResponse();
        r.setDrivers(driverService.getAllDrivers());
        r.setSuccess(true);
        return ResponseEntity.ok(r);
    }

    @Operation(summary = "Assign a driver to a bus")
    @PostMapping("/assignDriver/{busId}/{driverId}")
    public ResponseEntity<DriverResponse> assignDriver(
            @Parameter(description = "Bus UUID") @PathVariable UUID busId,
            @Parameter(description = "Driver UUID") @PathVariable UUID driverId) {
        Driver driver = driverService.findDriverbyid(driverId);
        if (driver == null) {
            DriverResponse r = new DriverResponse();
            r.setSuccess(false);
            r.setMessage("Driver not found");
            return new ResponseEntity<>(r, HttpStatus.NOT_FOUND);
        }
        Bus bus = busService.finderBusbyId(busId);
        if (bus == null) {
            DriverResponse r = new DriverResponse();
            r.setSuccess(false);
            r.setMessage("Bus not found");
            return new ResponseEntity<>(r, HttpStatus.NOT_FOUND);
        }
        driver.setBus(bus);
        Driver updated = driverService.addDriver(driver);
        DriverResponse r = new DriverResponse();
        r.setSuccess(updated != null);
        r.setMessage(updated != null ? "Driver assigned to bus " + bus.getBusno() : "Assignment failed");
        r.setDriver(updated);
        return new ResponseEntity<>(r, updated != null ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
