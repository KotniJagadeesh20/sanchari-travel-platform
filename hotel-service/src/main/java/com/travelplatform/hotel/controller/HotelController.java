package com.travelplatform.hotel.controller;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.travelplatform.hotel.dto.HotelResponse;
import com.travelplatform.hotel.entity.Hotel;
import com.travelplatform.hotel.enums.RoomType;
import com.travelplatform.hotel.service.HotelService;

/** Public hotel discovery — search and details. No auth required to browse. */
@RestController
@RequestMapping("/hotels")
@Validated
@Tag(name = "Hotels", description = "Search and view hotel details — public, no auth required")
public class HotelController {

    @Autowired private HotelService hotelService;

    @Operation(summary = "Search hotels", description = "checkIn/checkOut/guests narrow intent for future " +
            "per-night availability but currently only destinationId, starRating, roomType, minPrice and " +
            "maxPrice actually filter results (see Room.availableRooms Javadoc for why).")
    @GetMapping
    public ResponseEntity<Page<HotelResponse>> searchHotels(
            @RequestParam(required = false) UUID destinationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(required = false) Integer guests,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Integer starRating,
            @RequestParam(required = false) RoomType roomType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Hotel> hotels = hotelService.searchHotels(destinationId, starRating, roomType, minPrice, maxPrice, pageable);
        return ResponseEntity.ok(hotels.map(HotelResponse::from));
    }

    @Operation(summary = "Get hotel details",
            description = "Returns the hotel plus its rooms, amenities and images. Fetch reviews separately " +
                    "from GET /hotels/{hotelId}/reviews.")
    @GetMapping("/{hotelId}")
    public ResponseEntity<HotelResponse> getHotel(@Parameter(description = "Hotel UUID") @PathVariable UUID hotelId) {
        return ResponseEntity.ok(HotelResponse.withRooms(hotelService.getHotelById(hotelId)));
    }
}
