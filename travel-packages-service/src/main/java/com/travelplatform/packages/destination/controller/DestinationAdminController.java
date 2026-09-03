package com.travelplatform.packages.destination.controller;

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

import com.travelplatform.packages.destination.dto.CreateDestinationRequest;
import com.travelplatform.packages.destination.dto.DestinationDetailResponse;
import com.travelplatform.packages.destination.dto.UpdateDestinationRequest;
import com.travelplatform.packages.destination.entity.Destination;
import com.travelplatform.packages.destination.service.DestinationService;

/**
 * Admin-only destination curation. Enforced both at the gateway-trusted
 * header level (SecurityConfig: /destinations/admin/** requires ROLE_ADMIN)
 * and again here with @PreAuthorize for defense in depth — same pattern as
 * PackageAdminController.
 */
@RestController
@RequestMapping("/destinations/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@Validated
@Tag(name = "Destination Admin", description = "Create, update, and delist destinations — ROLE_ADMIN only")
@SecurityRequirement(name = "bearerAuth")
public class DestinationAdminController {

    @Autowired private DestinationService destinationService;

    @Operation(summary = "List all destinations including delisted ones (admin view)",
            description = "Without this, a delisted destination can never be found again to re-activate it — " +
                    "the customer-facing GET /destinations only returns active=true.")
    @GetMapping
    public ResponseEntity<List<DestinationDetailResponse>> getAllDestinations() {
        List<DestinationDetailResponse> destinations = destinationService.getAllDestinationsForAdmin()
                .stream().map(DestinationDetailResponse::from).collect(Collectors.toList());
        return ResponseEntity.ok(destinations);
    }

    @Operation(summary = "Create a destination", description = "Includes attractions and activities in one call.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Destination created"),
        @ApiResponse(responseCode = "400", description = "Validation failed")
    })
    @PostMapping
    public ResponseEntity<DestinationDetailResponse> createDestination(
            @Validated @RequestBody CreateDestinationRequest request) {
        Destination destination = destinationService.createDestination(request);
        return new ResponseEntity<>(DestinationDetailResponse.from(destination), HttpStatus.CREATED);
    }

    @Operation(summary = "Update a destination",
            description = "Only non-null fields are applied. If attractions/activities are provided, they fully replace the existing set.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Destination updated"),
        @ApiResponse(responseCode = "404", description = "Destination not found")
    })
    @PutMapping("/{destinationId}")
    public ResponseEntity<DestinationDetailResponse> updateDestination(
            @Parameter(description = "Destination UUID") @PathVariable UUID destinationId,
            @Validated @RequestBody UpdateDestinationRequest request) {
        Destination updated = destinationService.updateDestination(destinationId, request);
        return ResponseEntity.ok(DestinationDetailResponse.from(updated));
    }

    @Operation(summary = "Delist a destination",
            description = "Soft-delete: sets active=false. Preserves FK integrity for packages already linked via destinationId.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Destination delisted"),
        @ApiResponse(responseCode = "404", description = "Destination not found")
    })
    @DeleteMapping("/{destinationId}")
    public ResponseEntity<Void> deleteDestination(@PathVariable UUID destinationId) {
        destinationService.deleteDestination(destinationId);
        return ResponseEntity.noContent().build();
    }
}
