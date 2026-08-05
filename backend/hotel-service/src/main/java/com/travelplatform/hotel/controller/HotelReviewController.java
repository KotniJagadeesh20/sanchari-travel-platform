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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.travelplatform.hotel.dto.CreateReviewRequest;
import com.travelplatform.hotel.dto.ReviewResponse;
import com.travelplatform.hotel.entity.HotelReview;
import com.travelplatform.hotel.service.HotelReviewService;

@RestController
@RequestMapping("/hotels/{hotelId}/reviews")
@Validated
@Tag(name = "Hotel Reviews", description = "Rate and review hotels — reading is public, posting requires JWT")
public class HotelReviewController {

    @Autowired private HotelReviewService reviewService;

    @Operation(summary = "Get all reviews for a hotel")
    @GetMapping
    public ResponseEntity<List<ReviewResponse>> getReviews(@PathVariable UUID hotelId) {
        List<ReviewResponse> reviews = reviewService.getReviewsByHotel(hotelId)
                .stream().map(ReviewResponse::from).collect(Collectors.toList());
        return ResponseEntity.ok(reviews);
    }

    @Operation(summary = "Review a hotel", description = "One review per user per hotel.")
    @ApiResponse(responseCode = "400", description = "User has already reviewed this hotel")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @PathVariable UUID hotelId,
            @Validated @RequestBody CreateReviewRequest request,
            @Parameter(hidden = true) @RequestHeader("X-Authenticated-User-Id") String userIdStr) {

        HotelReview review = reviewService.createReview(hotelId, UUID.fromString(userIdStr), request);
        return new ResponseEntity<>(ReviewResponse.from(review), HttpStatus.CREATED);
    }
}
