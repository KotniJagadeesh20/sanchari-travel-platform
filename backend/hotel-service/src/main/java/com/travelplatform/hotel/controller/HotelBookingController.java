package com.travelplatform.hotel.controller;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
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

import com.travelplatform.hotel.dto.BookingResponse;
import com.travelplatform.hotel.dto.CreateBookingRequest;
import com.travelplatform.hotel.entity.HotelBooking;
import com.travelplatform.hotel.service.HotelBookingService;

/**
 * Hotel booking works independently of travel packages — a user can book a hotel
 * directly without ever touching travel-packages-service.
 */
@RestController
@RequestMapping("/hotel-bookings")
@Validated
@Tag(name = "Hotel Bookings", description = "Book and manage hotel bookings — requires JWT")
@SecurityRequirement(name = "bearerAuth")
public class HotelBookingController {

    @Autowired private HotelBookingService bookingService;

    @Operation(summary = "Book a hotel room",
            description = "Auto-confirms immediately (no payment step yet). Fails if the room has no " +
                    "available inventory or the date range is invalid.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Booking confirmed"),
        @ApiResponse(responseCode = "400", description = "Invalid dates or room not available"),
        @ApiResponse(responseCode = "404", description = "Hotel or room not found")
    })
    @PostMapping
    public ResponseEntity<BookingResponse> bookHotel(
            @Validated @RequestBody CreateBookingRequest request,
            @Parameter(hidden = true) @RequestHeader("X-Authenticated-User-Id") String userIdStr,
            @Parameter(hidden = true) @RequestHeader(value = "X-Authenticated-Email", required = false) String email) {

        HotelBooking booking = bookingService.bookHotel(request, UUID.fromString(userIdStr), email);
        return new ResponseEntity<>(BookingResponse.from(booking), HttpStatus.CREATED);
    }

    @Operation(summary = "Cancel my booking", description = "Returns the room to inventory if it wasn't already cancelled/checked out.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Booking cancelled"),
        @ApiResponse(responseCode = "403", description = "Caller is not this booking's owner"),
        @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    @DeleteMapping("/{bookingId}")
    public ResponseEntity<Void> cancelBooking(
            @PathVariable UUID bookingId,
            @Parameter(hidden = true) @RequestHeader("X-Authenticated-User-Id") String userIdStr,
            @Parameter(hidden = true) @RequestHeader(value = "X-Authenticated-Email", required = false) String email) {

        bookingService.cancelBooking(bookingId, UUID.fromString(userIdStr), email);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "List my hotel bookings")
    @GetMapping("/me")
    public ResponseEntity<List<BookingResponse>> getMyBookings(
            @Parameter(hidden = true) @RequestHeader("X-Authenticated-User-Id") String userIdStr) {

        List<BookingResponse> bookings = bookingService.getBookingsByUser(UUID.fromString(userIdStr))
                .stream().map(BookingResponse::from).collect(Collectors.toList());
        return ResponseEntity.ok(bookings);
    }
}
