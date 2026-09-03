package com.travelplatform.packages.controller;

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

import com.travelplatform.packages.dto.BookPackageRequest;
import com.travelplatform.packages.dto.PackageBookingResponse;
import com.travelplatform.packages.dto.PackageResponse;
import com.travelplatform.packages.entity.PackageBooking;
import com.travelplatform.packages.entity.UserRef;
import com.travelplatform.packages.service.PackageBookingService;
import com.travelplatform.packages.service.PackageService;
import com.travelplatform.packages.service.UserRefService;

/**
 * Traveler-facing package browsing and booking. Any authenticated user
 * (ROLE_USER or ROLE_ADMIN) can browse and book — there's no separate
 * "traveler role"; admin-curation happens under /packages/admin/** instead.
 */
@RestController
@RequestMapping("/packages")
@Validated
@Tag(name = "Packages", description = "Browse and book travel packages — requires JWT")
@SecurityRequirement(name = "bearerAuth")
public class PackageController {

    @Autowired private PackageService packageService;
    @Autowired private PackageBookingService bookingService;
    @Autowired private UserRefService userRefService;

    @Operation(summary = "Browse all listed packages")
    @GetMapping
    public ResponseEntity<List<PackageResponse>> getAllPackages() {
        List<PackageResponse> packages = packageService.getAllActivePackages()
                .stream().map(PackageResponse::from).collect(Collectors.toList());
        return ResponseEntity.ok(packages);
    }

    @Operation(summary = "Search packages",
            description = "Provide any combination of destinationId, keyword, maxBudget, minDurationDays, and " +
                    "maxDurationDays — all provided filters are applied together (AND), not just whichever one " +
                    "happens to be set. keyword matches against title or description.")
    @GetMapping("/search")
    public ResponseEntity<List<PackageResponse>> search(
            @RequestParam(required = false) UUID destinationId,
            @Parameter(description = "Partial, case-insensitive match against title or description") @RequestParam(required = false) String keyword,
            @Parameter(description = "Maximum package price") @RequestParam(required = false) Double maxBudget,
            @RequestParam(required = false) Integer minDurationDays,
            @RequestParam(required = false) Integer maxDurationDays) {

        List<PackageResponse> results = packageService.search(destinationId, keyword, maxBudget, minDurationDays, maxDurationDays)
                .stream().map(PackageResponse::from).collect(Collectors.toList());
        return ResponseEntity.ok(results);
    }

    @Operation(
        summary = "Find packages by destination",
        description = "Returns all active packages linked to the given destination UUID. " +
                "Use GET /destinations/search to find destination UUIDs first."
    )
    @GetMapping("/by-destination/{destinationId}")
    public ResponseEntity<List<PackageResponse>> getPackagesByDestination(
            @Parameter(description = "Destination UUID") @PathVariable UUID destinationId) {
        List<PackageResponse> packages = packageService.findByDestination(destinationId)
                .stream().map(PackageResponse::from).collect(Collectors.toList());
        return ResponseEntity.ok(packages);
    }

    @Operation(summary = "Get package details by ID",
            description = "Response includes all of the package's departure batches (see departures[]) — " +
                    "pick one's id to pass to POST /packages/departures/{departureId}/book.")
    @ApiResponse(responseCode = "404", description = "Package not found")
    @GetMapping("/{packageId}")
    public ResponseEntity<PackageResponse> getPackage(@PathVariable UUID packageId) {
        return ResponseEntity.ok(PackageResponse.from(packageService.getPackageById(packageId)));
    }

    @Operation(summary = "Book a specific departure batch of a package",
            description = "Auto-confirms immediately (no approval step). Fails if the package/departure isn't " +
                    "bookable or the traveler count exceeds that departure's availableSlots. paymentStatus starts " +
                    "PENDING — no payment gateway is integrated yet.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Booking confirmed"),
        @ApiResponse(responseCode = "400", description = "Not bookable or insufficient slots"),
        @ApiResponse(responseCode = "404", description = "Departure not found")
    })
    @PostMapping("/departures/{departureId}/book")
    public ResponseEntity<PackageBookingResponse> bookPackage(
            @Parameter(description = "PackageDeparture UUID — from a package's departures[] list") @PathVariable UUID departureId,
            @Validated @RequestBody BookPackageRequest request,
            @Parameter(hidden = true) @RequestHeader("X-Authenticated-Email") String email,
            @Parameter(hidden = true) @RequestHeader("X-Authenticated-Name") String name,
            @Parameter(hidden = true) @RequestHeader("X-Authenticated-User-Id") String userIdStr) {

        UserRef traveler = userRefService.findOrCreate(UUID.fromString(userIdStr), email, name);
        PackageBooking booking = bookingService.bookPackage(departureId, request.getTravelers(), traveler);
        return new ResponseEntity<>(PackageBookingResponse.from(booking), HttpStatus.CREATED);
    }

    @Operation(summary = "List my package bookings")
    @GetMapping("/bookings")
    public ResponseEntity<List<PackageBookingResponse>> getMyBookings(
            @Parameter(hidden = true) @RequestHeader("X-Authenticated-User-Id") String userIdStr) {
        List<PackageBookingResponse> bookings = bookingService.getBookingsByTraveler(UUID.fromString(userIdStr))
                .stream().map(PackageBookingResponse::from).collect(Collectors.toList());
        return ResponseEntity.ok(bookings);
    }

    @Operation(summary = "Cancel my booking", description = "Returns slots to the departure if it was CONFIRMED.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Booking cancelled"),
        @ApiResponse(responseCode = "403", description = "Caller is not this booking's traveler"),
        @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    @DeleteMapping("/bookings/{bookingId}")
    public ResponseEntity<Void> cancelBooking(
            @Parameter(description = "Booking UUID") @PathVariable UUID bookingId,
            @Parameter(description = "Optional reason for cancelling") @RequestParam(required = false) String reason,
            @Parameter(hidden = true) @RequestHeader("X-Authenticated-User-Id") String userIdStr) {

        bookingService.cancelBooking(bookingId, UUID.fromString(userIdStr), reason);
        return ResponseEntity.noContent().build();
    }
}
