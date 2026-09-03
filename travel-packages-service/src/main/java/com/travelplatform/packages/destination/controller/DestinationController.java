package com.travelplatform.packages.destination.controller;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.travelplatform.packages.destination.dto.DestinationDetailResponse;
import com.travelplatform.packages.destination.dto.DestinationSummaryResponse;
import com.travelplatform.packages.destination.enums.DestinationCategory;
import com.travelplatform.packages.destination.service.DestinationService;

/**
 * Traveler-facing destination discovery: browse, filter by category, search
 * by keyword/budget, view popular destinations, and view full detail pages.
 * Answers "Where should I go?" — not a booking flow.
 */
@RestController
@RequestMapping("/destinations")
@Validated
@Tag(name = "Destinations", description = "Browse and search destinations — requires JWT")
@SecurityRequirement(name = "bearerAuth")
public class DestinationController {

    @Autowired private DestinationService destinationService;

    @Operation(summary = "Browse all listed destinations")
    @GetMapping
    public ResponseEntity<List<DestinationSummaryResponse>> getAllDestinations() {
        List<DestinationSummaryResponse> destinations = destinationService.getAllActiveDestinations()
                .stream().map(DestinationSummaryResponse::from).collect(Collectors.toList());
        return ResponseEntity.ok(destinations);
    }

    @Operation(summary = "Get full destination details by ID",
            description = "Includes attractions, activities, and images — used for the destination detail page.")
    @ApiResponse(responseCode = "404", description = "Destination not found")
    @GetMapping("/{destinationId}")
    public ResponseEntity<DestinationDetailResponse> getDestination(@PathVariable UUID destinationId) {
        return ResponseEntity.ok(DestinationDetailResponse.from(destinationService.getDestinationById(destinationId)));
    }

    @Operation(summary = "Search destinations",
            description = "Provide any combination of keyword, category, maxBudget, and visitMonth (1=Jan..12=Dec) " +
                    "— all provided filters are applied together (AND), not just whichever one happens to be set.")
    @GetMapping("/search")
    public ResponseEntity<List<DestinationSummaryResponse>> search(
            @Parameter(description = "Partial, case-insensitive name match") @RequestParam(required = false) String keyword,
            @RequestParam(required = false) DestinationCategory category,
            @Parameter(description = "Maximum average budget") @RequestParam(required = false) Double maxBudget,
            @Parameter(description = "1=January..12=December — matches destinations whose bestMonths includes this month")
            @RequestParam(required = false) Integer visitMonth) {

        List<DestinationSummaryResponse> results = destinationService.search(keyword, category, maxBudget, visitMonth)
                .stream().map(DestinationSummaryResponse::from).collect(Collectors.toList());
        return ResponseEntity.ok(results);
    }

    @Operation(summary = "Get destinations by category")
    @GetMapping("/category/{category}")
    public ResponseEntity<List<DestinationSummaryResponse>> getByCategory(@PathVariable DestinationCategory category) {
        List<DestinationSummaryResponse> destinations = destinationService.search(null, category, null, null)
                .stream().map(DestinationSummaryResponse::from).collect(Collectors.toList());
        return ResponseEntity.ok(destinations);
    }

    @Operation(summary = "Get popular destinations", description = "Ordered by rating descending.")
    @GetMapping("/popular")
    public ResponseEntity<List<DestinationSummaryResponse>> getPopularDestinations() {
        List<DestinationSummaryResponse> destinations = destinationService.getPopularDestinations()
                .stream().map(DestinationSummaryResponse::from).collect(Collectors.toList());
        return ResponseEntity.ok(destinations);
    }
}
