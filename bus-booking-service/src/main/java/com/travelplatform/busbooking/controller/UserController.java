package com.travelplatform.busbooking.controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.travelplatform.busbooking.dto.BookingRequest;
import com.travelplatform.busbooking.dto.BusResponse;
import com.travelplatform.busbooking.entity.Bookingdetails;
import com.travelplatform.busbooking.entity.Bus;
import com.travelplatform.busbooking.entity.UserAdmin;
import com.travelplatform.busbooking.repository.BusRepository;
import com.travelplatform.busbooking.service.BookingDetailsService;
import com.travelplatform.busbooking.service.BusService;
import com.travelplatform.busbooking.service.UserAdminService;

@RestController
@RequestMapping("/api/user")
@Validated
@Tag(name = "User", description = "Bus search, booking, and history — requires JWT")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    @Autowired private BusService busService;
    @Autowired private BookingDetailsService bookingService;
    @Autowired private BusRepository busRepository;
    @Autowired private UserAdminService userAdminService;

    // ─── Search ─────────────────────────────────────────────────────────────

    @Operation(summary = "Search buses by route and date")
    @ApiResponse(responseCode = "200", description = "Results returned (may be empty)")
    @GetMapping("/searchbusses/{source}/{destination}/{date}")
    public ResponseEntity<BusResponse> searchBusses(
            @PathVariable String source,
            @PathVariable String destination,
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        BusResponse r = new BusResponse();
        r.setBusses(busService.Searchbus(source, destination, date));
        r.setSuccess(true);
        return ResponseEntity.ok(r);
    }

    // ─── Book ────────────────────────────────────────────────────────────────

    @Operation(summary = "Book a ticket",
        description = "User identity is read from the X-Authenticated-Email and X-Authenticated-User-Id " +
                      "headers set by the API Gateway — clients do not supply userId in the URL.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Booked — bookingId (UUID) returned"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "404", description = "Bus not found")
    })
    @PostMapping("/bookticket/{busId}")
    public ResponseEntity<Map<String, Object>> bookTicket(
            @Parameter(description = "Bus UUID") @PathVariable UUID busId,
            @Validated @RequestBody BookingRequest request,
            @Parameter(hidden = true) @RequestHeader("X-Authenticated-Email") String userEmail,
            @Parameter(hidden = true) @RequestHeader("X-Authenticated-User-Id") String userIdStr) {

        Map<String, Object> response = new HashMap<>();

        Bus bus = busRepository.findById(busId).orElse(null);
        if (bus == null) {
            response.put("success", false);
            response.put("message", "Bus not found: " + busId);
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        // Resolve or create lightweight user_ref record from gateway headers
        UUID userId = UUID.fromString(userIdStr);
        UserAdmin user = userAdminService.findOrCreate(userId, userEmail);

        Bookingdetails booking = new Bookingdetails();
        booking.setName(request.getName());
        booking.setEmail(request.getEmail());
        booking.setPhoneno(request.getPhoneno());
        booking.setAge(request.getAge());
        booking.setPrice(bus.getPrice());
        booking.setPaymentdate(new java.util.Date());
        booking.setBus(bus);
        booking.setUser(user);

        Bookingdetails saved = bookingService.savebookingdetails(booking);
        if (saved != null) {
            response.put("success", true);
            response.put("message", "Ticket booked");
            response.put("bookingId", saved.getId());
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        }
        response.put("success", false);
        response.put("message", "Booking failed");
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ─── History ────────────────────────────────────────────────────────────

    @Operation(summary = "Get booking history for the authenticated user")
    @GetMapping("/bookingDetails")
    public ResponseEntity<?> bookingDetails(
            @Parameter(hidden = true) @RequestHeader("X-Authenticated-User-Id") String userIdStr) {

        UUID userId = UUID.fromString(userIdStr);
        List<Object> details = bookingService.getBookingDetails(userId);
        Map<String, Object> response = new HashMap<>();
        if (details != null && !details.isEmpty()) {
            response.put("success", true);
            response.put("bookingDetails", details);
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("message", "No bookings found for user");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // ─── Cancel ─────────────────────────────────────────────────────────────

    @Operation(summary = "Cancel a booking by booking UUID",
        description = "Only the user who made the booking may cancel it. Ownership is verified against " +
                      "the X-Authenticated-User-Id header set by the API Gateway.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cancelled"),
        @ApiResponse(responseCode = "403", description = "Booking belongs to a different user"),
        @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    @DeleteMapping("/cancelbooking/{id}")
    public ResponseEntity<?> cancelTicket(
            @Parameter(description = "Booking UUID") @PathVariable UUID id,
            @Parameter(hidden = true) @RequestHeader("X-Authenticated-User-Id") String userIdStr) {
        UUID requestingUserId = UUID.fromString(userIdStr);
        boolean cancelled = bookingService.cancelTickets(id, requestingUserId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", cancelled);
        response.put("message", cancelled ? "Booking cancelled" : "Booking not found");
        return new ResponseEntity<>(response, cancelled ? HttpStatus.OK : HttpStatus.NOT_FOUND);
    }
}
