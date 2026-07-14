package com.travelplatform.packages.controller;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.travelplatform.packages.dto.CreatePackageRequest;
import com.travelplatform.packages.dto.DepartureRequest;
import com.travelplatform.packages.dto.DepartureResponse;
import com.travelplatform.packages.dto.PackageBookingResponse;
import com.travelplatform.packages.dto.PackageResponse;
import com.travelplatform.packages.dto.UpdatePackageRequest;
import com.travelplatform.packages.entity.PackageDeparture;
import com.travelplatform.packages.entity.TravelPackage;
import com.travelplatform.packages.entity.UserRef;
import com.travelplatform.packages.service.PackageBookingService;
import com.travelplatform.packages.service.PackageService;
import com.travelplatform.packages.service.UserRefService;

/**
 * Admin-only package curation: create, update, delist, and view all
 * bookings on a package. Enforced both at the gateway-trusted header
 * level (SecurityConfig: /packages/admin/** requires ROLE_ADMIN) and
 * again here with @PreAuthorize for defense in depth.
 */
@RestController
@RequestMapping("/packages/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@Validated
@Tag(name = "Package Admin", description = "Create, update, and delist travel packages — ROLE_ADMIN only")
@SecurityRequirement(name = "bearerAuth")
public class PackageAdminController {

    @Autowired private PackageService packageService;
    @Autowired private PackageBookingService bookingService;
    @Autowired private UserRefService userRefService;

    @Operation(summary = "Create a travel package")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Package created"),
        @ApiResponse(responseCode = "400", description = "Validation failed")
    })
    @PostMapping
    public ResponseEntity<PackageResponse> createPackage(
            @Validated @RequestBody CreatePackageRequest request,
            @Parameter(hidden = true) @RequestHeader("X-Authenticated-Email") String email,
            @Parameter(hidden = true) @RequestHeader("X-Authenticated-Name") String name,
            @Parameter(hidden = true) @RequestHeader("X-Authenticated-User-Id") String userIdStr) {
        UserRef creator = userRefService.findOrCreate(UUID.fromString(userIdStr), email, name);
        TravelPackage pkg = packageService.createPackage(request, creator);
        return new ResponseEntity<>(PackageResponse.from(pkg), HttpStatus.CREATED);
    }

    @Operation(summary = "Update a travel package", description = "Only non-null fields in the request are applied.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Package updated"),
        @ApiResponse(responseCode = "404", description = "Package not found")
    })
    @PutMapping("/{packageId}")
    public ResponseEntity<PackageResponse> updatePackage(
            @Parameter(description = "Package UUID") @PathVariable UUID packageId,
            @Validated @RequestBody UpdatePackageRequest request) {
        TravelPackage updated = packageService.updatePackage(packageId, request);
        return ResponseEntity.ok(PackageResponse.from(updated));
    }

    @Operation(summary = "Delist a package", description = "Soft-delete: sets active=false. Booking history is preserved.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Package delisted"),
        @ApiResponse(responseCode = "404", description = "Package not found")
    })
    @DeleteMapping("/{packageId}")
    public ResponseEntity<Void> deletePackage(@PathVariable UUID packageId) {
        packageService.deletePackage(packageId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "List all packages including delisted ones (admin view)")
    @GetMapping
    public ResponseEntity<List<PackageResponse>> getAllPackages() {
        List<PackageResponse> packages = packageService.getAllPackagesForAdmin()
                .stream().map(PackageResponse::from).collect(Collectors.toList());
        return ResponseEntity.ok(packages);
    }

    @Operation(summary = "List packages I created",
            description = "Scoped to the caller's own packages via createdBy. Note: this doesn't restrict " +
                    "editing/delisting to the creator — any ROLE_ADMIN can still manage any package until the " +
                    "full ROLE_PARTNER ownership model lands. This just scopes the listing view.")
    @GetMapping("/mine")
    public ResponseEntity<List<PackageResponse>> getMyPackages(
            @Parameter(hidden = true) @RequestHeader("X-Authenticated-User-Id") String userIdStr) {
        List<PackageResponse> packages = packageService.getPackagesByCreator(UUID.fromString(userIdStr))
                .stream().map(PackageResponse::from).collect(Collectors.toList());
        return ResponseEntity.ok(packages);
    }

    @Operation(summary = "View all bookings on a package")
    @ApiResponse(responseCode = "404", description = "Package not found")
    @GetMapping("/{packageId}/bookings")
    public ResponseEntity<List<PackageBookingResponse>> getBookingsForPackage(@PathVariable UUID packageId) {
        List<PackageBookingResponse> bookings = bookingService.getBookingsByPackage(packageId)
                .stream().map(PackageBookingResponse::from).collect(Collectors.toList());
        return ResponseEntity.ok(bookings);
    }

    @Operation(summary = "Add a bookable departure batch to a package",
            description = "maxPeople defaults to the package's own maxPeople if omitted.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Departure added"),
        @ApiResponse(responseCode = "404", description = "Package not found")
    })
    @PostMapping("/{packageId}/departures")
    public ResponseEntity<DepartureResponse> addDeparture(
            @PathVariable UUID packageId,
            @Validated @RequestBody DepartureRequest request) {
        PackageDeparture departure = packageService.addDeparture(packageId, request);
        return new ResponseEntity<>(DepartureResponse.from(departure), HttpStatus.CREATED);
    }

    @Operation(summary = "Edit a departure's date/capacity",
            description = "Changing maxPeople shifts availableSlots by the same delta — already-booked travelers stay booked.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Departure updated"),
        @ApiResponse(responseCode = "404", description = "Departure not found")
    })
    @PutMapping("/departures/{departureId}")
    public ResponseEntity<DepartureResponse> updateDeparture(
            @PathVariable UUID departureId,
            @Validated @RequestBody DepartureRequest request) {
        PackageDeparture departure = packageService.updateDeparture(departureId, request);
        return ResponseEntity.ok(DepartureResponse.from(departure));
    }

    @Operation(summary = "Cancel one departure batch",
            description = "Soft-cancel (active=false) — only this specific date, not the whole package template or its other departures.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Departure cancelled"),
        @ApiResponse(responseCode = "404", description = "Departure not found")
    })
    @DeleteMapping("/departures/{departureId}")
    public ResponseEntity<Void> cancelDeparture(@PathVariable UUID departureId) {
        packageService.cancelDeparture(departureId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Cancel a customer's booking (operator-initiated)",
            description = "For trips called off by the operator (weather, under-booking, etc). No ownership check — " +
                    "same as every other admin-scoped action here; restricting this to the package's own creator " +
                    "is a ROLE_PARTNER-era enforcement, not something added now.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Booking cancelled"),
        @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    @PostMapping("/bookings/{bookingId}/cancel")
    public ResponseEntity<Void> cancelBookingAsAdmin(
            @PathVariable UUID bookingId,
            @Parameter(description = "Optional reason shown to the traveler") @RequestParam(required = false) String reason) {
        bookingService.cancelBookingAsAdmin(bookingId, reason);
        return ResponseEntity.noContent().build();
    }
}
