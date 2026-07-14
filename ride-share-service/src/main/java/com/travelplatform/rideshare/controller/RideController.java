package com.travelplatform.rideshare.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.travelplatform.rideshare.dto.*;
import com.travelplatform.rideshare.entity.Ride;
import com.travelplatform.rideshare.entity.RideBooking;
import com.travelplatform.rideshare.entity.UserRef;
import com.travelplatform.rideshare.service.RideBookingService;
import com.travelplatform.rideshare.service.RideService;
import com.travelplatform.rideshare.service.UserRefService;

/**
 * All ride and booking operations under /rides/**.
 * There is no ROLE_DRIVER — any authenticated user can create a ride
 * (becoming that ride's driver) and separately book seats on other
 * users' rides as a passenger. Identity comes from X-Authenticated-Email
 * and X-Authenticated-User-Id headers forwarded by the API Gateway.
 */
@RestController
@RequestMapping("/rides")
@Validated
@Tag(name = "Rides", description = "Peer-to-peer ride sharing — create, search, book, approve/reject, cancel")
@SecurityRequirement(name = "bearerAuth")
public class RideController {

    @Autowired private RideService rideService;
    @Autowired private RideBookingService bookingService;
    @Autowired private UserRefService userRefService;

    // ─── Driver: create / update / cancel / my rides ────────────────────────

    @Operation(summary = "Create a ride", description = "Posts a new ride offer. The caller becomes the ride's driver.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Ride created"),
        @ApiResponse(responseCode = "400", description = "Validation failed")
    })
    @PostMapping
    public ResponseEntity<RideResponse> createRide(
            @Validated @RequestBody CreateRideRequest request,
            @Parameter(hidden = true) @RequestHeader("X-Authenticated-Email") String email,
            @Parameter(hidden = true) @RequestHeader("X-Authenticated-Name") String name,
            @Parameter(hidden = true) @RequestHeader("X-Authenticated-User-Id") String userIdStr) {

        UserRef driver = userRefService.findOrCreate(UUID.fromString(userIdStr), email, name);
        Ride ride = rideService.createRide(request, driver);
        return new ResponseEntity<>(RideResponse.from(ride), HttpStatus.CREATED);
    }

    @Operation(summary = "Update a ride", description = "Only the ride's driver may update it.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ride updated"),
        @ApiResponse(responseCode = "403", description = "Caller is not the ride's driver"),
        @ApiResponse(responseCode = "404", description = "Ride not found")
    })
    @PutMapping("/{rideId}")
    public ResponseEntity<RideResponse> updateRide(
            @Parameter(description = "Ride UUID") @PathVariable UUID rideId,
            @Validated @RequestBody UpdateRideRequest request,
            @Parameter(hidden = true) @RequestHeader("X-Authenticated-User-Id") String userIdStr) {

        Ride updated = rideService.updateRide(rideId, request, UUID.fromString(userIdStr));
        return ResponseEntity.ok(RideResponse.from(updated));
    }

    @Operation(summary = "Cancel a ride", description = "Only the ride's driver may cancel it. Existing bookings remain on record.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Ride cancelled"),
        @ApiResponse(responseCode = "403", description = "Caller is not the ride's driver"),
        @ApiResponse(responseCode = "404", description = "Ride not found")
    })
    @DeleteMapping("/{rideId}")
    public ResponseEntity<Void> cancelRide(
            @Parameter(description = "Ride UUID") @PathVariable UUID rideId,
            @Parameter(hidden = true) @RequestHeader("X-Authenticated-User-Id") String userIdStr) {

        rideService.cancelRide(rideId, UUID.fromString(userIdStr));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "List rides I created (as driver)")
    @GetMapping("/driver")
    public ResponseEntity<List<RideResponse>> getMyRidesAsDriver(
            @Parameter(hidden = true) @RequestHeader("X-Authenticated-User-Id") String userIdStr) {

        List<RideResponse> rides = rideService.getRidesByDriver(UUID.fromString(userIdStr))
                .stream().map(RideResponse::from).collect(Collectors.toList());
        return ResponseEntity.ok(rides);
    }

    // ─── Passenger: search / view / book / my bookings / cancel ────────────

    @Operation(summary = "Search rides", description = "Returns SCHEDULED rides matching source, destination, and date.")
    @GetMapping("/search")
    public ResponseEntity<List<RideResponse>> searchRides(
            @RequestParam String source,
            @RequestParam String destination,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {

        List<RideResponse> rides = rideService.searchRides(source, destination, date)
                .stream().map(RideResponse::from).collect(Collectors.toList());
        return ResponseEntity.ok(rides);
    }

    @Operation(summary = "Get ride details by ID")
    @ApiResponse(responseCode = "404", description = "Ride not found")
    @GetMapping("/{rideId}")
    public ResponseEntity<RideResponse> getRide(@PathVariable UUID rideId) {
        return ResponseEntity.ok(RideResponse.from(rideService.getRideById(rideId)));
    }

    @Operation(summary = "Book seats on a ride",
            description = "Creates a PENDING booking awaiting driver approval. " +
                    "Fails if you are the ride's driver, the ride isn't SCHEDULED, " +
                    "or requested seats exceed availability.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Booking created as PENDING"),
        @ApiResponse(responseCode = "400", description = "Own-ride booking, insufficient seats, or ride not bookable"),
        @ApiResponse(responseCode = "404", description = "Ride not found")
    })
    @PostMapping("/{rideId}/book")
    public ResponseEntity<RideBookingResponse> bookRide(
            @Parameter(description = "Ride UUID") @PathVariable UUID rideId,
            @Validated @RequestBody BookRideRequest request,
            @Parameter(hidden = true) @RequestHeader("X-Authenticated-Email") String email,
            @Parameter(hidden = true) @RequestHeader("X-Authenticated-Name") String name,
            @Parameter(hidden = true) @RequestHeader("X-Authenticated-User-Id") String userIdStr) {

        UserRef passenger = userRefService.findOrCreate(UUID.fromString(userIdStr), email, name);
        RideBooking booking = bookingService.bookRide(rideId, request.getSeats(), passenger);
        return new ResponseEntity<>(RideBookingResponse.from(booking), HttpStatus.CREATED);
    }

    @Operation(summary = "List my bookings (as passenger)")
    @GetMapping("/bookings")
    public ResponseEntity<List<RideBookingResponse>> getMyBookings(
            @Parameter(hidden = true) @RequestHeader("X-Authenticated-User-Id") String userIdStr) {

        List<RideBookingResponse> bookings = bookingService.getBookingsByPassenger(UUID.fromString(userIdStr))
                .stream().map(RideBookingResponse::from).collect(Collectors.toList());
        return ResponseEntity.ok(bookings);
    }

    @Operation(summary = "Cancel my booking", description = "Only the passenger who made the booking may cancel it. Returns seats if it was APPROVED.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Booking cancelled"),
        @ApiResponse(responseCode = "403", description = "Caller is not this booking's passenger"),
        @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    @DeleteMapping("/bookings/{bookingId}")
    public ResponseEntity<Void> cancelBooking(
            @Parameter(description = "Booking UUID") @PathVariable UUID bookingId,
            @Parameter(hidden = true) @RequestHeader("X-Authenticated-User-Id") String userIdStr) {

        bookingService.cancelBooking(bookingId, UUID.fromString(userIdStr));
        return ResponseEntity.noContent().build();
    }

    // ─── Driver: approve / reject bookings on my rides ──────────────────────

    @Operation(summary = "List bookings received on one of my rides", description = "Driver-only — returns 403 if the caller doesn't own the ride.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Bookings returned"),
        @ApiResponse(responseCode = "403", description = "Caller is not the ride's driver"),
        @ApiResponse(responseCode = "404", description = "Ride not found")
    })
    @GetMapping("/{rideId}/bookings")
    public ResponseEntity<List<RideBookingResponse>> getBookingsForRide(
            @PathVariable UUID rideId,
            @Parameter(hidden = true) @RequestHeader("X-Authenticated-User-Id") String userIdStr) {
        List<RideBookingResponse> bookings = bookingService.getBookingsByRide(rideId, UUID.fromString(userIdStr))
                .stream().map(RideBookingResponse::from).collect(Collectors.toList());
        return ResponseEntity.ok(bookings);
    }

    @Operation(summary = "Approve a pending booking", description = "Only the ride's driver may approve. Deducts seats from availability.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Booking approved"),
        @ApiResponse(responseCode = "400", description = "Seats no longer available"),
        @ApiResponse(responseCode = "403", description = "Caller is not the ride's driver, or booking isn't PENDING")
    })
    @PostMapping("/bookings/{bookingId}/approve")
    public ResponseEntity<RideBookingResponse> approveBooking(
            @PathVariable UUID bookingId,
            @Parameter(hidden = true) @RequestHeader("X-Authenticated-User-Id") String userIdStr) {

        RideBooking booking = bookingService.approveBooking(bookingId, UUID.fromString(userIdStr));
        return ResponseEntity.ok(RideBookingResponse.from(booking));
    }

    @Operation(summary = "Reject a pending booking", description = "Only the ride's driver may reject. No seats are deducted.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Booking rejected"),
        @ApiResponse(responseCode = "403", description = "Caller is not the ride's driver, or booking isn't PENDING")
    })
    @PostMapping("/bookings/{bookingId}/reject")
    public ResponseEntity<RideBookingResponse> rejectBooking(
            @PathVariable UUID bookingId,
            @Parameter(hidden = true) @RequestHeader("X-Authenticated-User-Id") String userIdStr) {

        RideBooking booking = bookingService.rejectBooking(bookingId, UUID.fromString(userIdStr));
        return ResponseEntity.ok(RideBookingResponse.from(booking));
    }
}
