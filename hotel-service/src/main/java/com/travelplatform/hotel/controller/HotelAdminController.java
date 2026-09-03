package com.travelplatform.hotel.controller;

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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.travelplatform.hotel.dto.*;
import com.travelplatform.hotel.entity.Hotel;
import com.travelplatform.hotel.entity.Room;
import com.travelplatform.hotel.entity.UserRef;
import com.travelplatform.hotel.service.HotelService;
import com.travelplatform.hotel.service.HotelBookingService;
import com.travelplatform.hotel.service.RoomService;
import com.travelplatform.hotel.service.UserRefService;

/**
 * Admin-only hotel and room management. Enforced both at the gateway-trusted
 * header level (SecurityConfig: /hotels/admin/** requires ROLE_ADMIN) and again
 * here with @PreAuthorize for defense in depth — matches travel-packages-service's
 * PackageAdminController.
 */
@RestController
@RequestMapping("/hotels/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@Validated
@Tag(name = "Hotel Admin", description = "Create/update/delete hotels, rooms, images, amenities — requires ROLE_ADMIN")
@SecurityRequirement(name = "bearerAuth")
public class HotelAdminController {

    @Autowired private HotelService hotelService;
    @Autowired private HotelBookingService bookingService;
    @Autowired private RoomService roomService;
    @Autowired private UserRefService userRefService;

    @Operation(summary = "Create a hotel")
    @PostMapping
    public ResponseEntity<HotelResponse> createHotel(
            @Validated @RequestBody CreateHotelRequest request,
            @Parameter(hidden = true) @RequestHeader("X-Authenticated-Email") String email,
            @Parameter(hidden = true) @RequestHeader("X-Authenticated-Name") String name,
            @Parameter(hidden = true) @RequestHeader("X-Authenticated-User-Id") String userIdStr) {
        UserRef creator = userRefService.findOrCreate(UUID.fromString(userIdStr), email, name);
        Hotel hotel = hotelService.createHotel(request, creator);
        return new ResponseEntity<>(HotelResponse.from(hotel), HttpStatus.CREATED);
    }

    @Operation(summary = "Update a hotel (partial)")
    @PutMapping("/{hotelId}")
    public ResponseEntity<HotelResponse> updateHotel(
            @PathVariable UUID hotelId, @Validated @RequestBody UpdateHotelRequest request) {
        return ResponseEntity.ok(HotelResponse.from(hotelService.updateHotel(hotelId, request)));
    }

    @Operation(summary = "Delist a hotel", description = "Soft delete — sets active=false. Booking/review history is preserved.")
    @DeleteMapping("/{hotelId}")
    public ResponseEntity<Void> deleteHotel(@PathVariable UUID hotelId) {
        hotelService.deleteHotel(hotelId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "List all hotels including delisted ones (admin view)")
    @GetMapping
    public ResponseEntity<List<HotelResponse>> getAllHotels() {
        List<HotelResponse> hotels = hotelService.getAllHotelsForAdmin()
                .stream().map(HotelResponse::from).collect(Collectors.toList());
        return ResponseEntity.ok(hotels);
    }

    @Operation(summary = "List hotels I created",
            description = "Scoped to the caller's own hotels via createdBy. Note: this doesn't restrict " +
                    "editing/managing to the creator — any ROLE_ADMIN can still manage any hotel until the " +
                    "full ROLE_PARTNER ownership model lands. This just scopes the listing view.")
    @GetMapping("/mine")
    public ResponseEntity<List<HotelResponse>> getMyHotels(
            @Parameter(hidden = true) @RequestHeader("X-Authenticated-User-Id") String userIdStr) {
        List<HotelResponse> hotels = hotelService.getHotelsByCreator(UUID.fromString(userIdStr))
                .stream().map(HotelResponse::from).collect(Collectors.toList());
        return ResponseEntity.ok(hotels);
    }

    @Operation(summary = "List all bookings for one hotel",
            description = "Across all of the hotel's rooms. No ownership check — any ROLE_ADMIN can view any " +
                    "hotel's bookings, same as every other admin action here until ROLE_PARTNER lands.")
    @GetMapping("/{hotelId}/bookings")
    public ResponseEntity<List<BookingResponse>> getBookingsForHotel(@PathVariable UUID hotelId) {
        List<BookingResponse> bookings = bookingService.getBookingsByHotel(hotelId)
                .stream().map(BookingResponse::from).collect(Collectors.toList());
        return ResponseEntity.ok(bookings);
    }

    @Operation(summary = "Add an image to a hotel")
    @PostMapping("/{hotelId}/images")
    public ResponseEntity<Void> addHotelImage(
            @PathVariable UUID hotelId, @Validated @RequestBody AddImageRequest request) {
        hotelService.addImage(hotelId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Add an amenity to a hotel")
    @PostMapping("/{hotelId}/amenities")
    public ResponseEntity<Void> addHotelAmenity(
            @PathVariable UUID hotelId, @Validated @RequestBody AddAmenityRequest request) {
        hotelService.addAmenity(hotelId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Add a room to a hotel")
    @PostMapping("/{hotelId}/rooms")
    public ResponseEntity<RoomResponse> addRoom(
            @PathVariable UUID hotelId, @Validated @RequestBody CreateRoomRequest request) {
        Room room = roomService.addRoom(hotelId, request);
        return new ResponseEntity<>(RoomResponse.from(room), HttpStatus.CREATED);
    }

    @Operation(summary = "Update a room (partial)")
    @PutMapping("/rooms/{roomId}")
    public ResponseEntity<RoomResponse> updateRoom(
            @PathVariable UUID roomId, @Validated @RequestBody UpdateRoomRequest request) {
        return ResponseEntity.ok(RoomResponse.from(roomService.updateRoom(roomId, request)));
    }

    @Operation(summary = "Delete (delist) a room")
    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<Void> deleteRoom(@PathVariable UUID roomId) {
        roomService.deleteRoom(roomId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Add an image to a room")
    @PostMapping("/rooms/{roomId}/images")
    public ResponseEntity<Void> addRoomImage(
            @PathVariable UUID roomId, @Validated @RequestBody AddImageRequest request) {
        roomService.addImage(roomId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Add an amenity to a room")
    @PostMapping("/rooms/{roomId}/amenities")
    public ResponseEntity<Void> addRoomAmenity(
            @PathVariable UUID roomId, @Validated @RequestBody AddAmenityRequest request) {
        roomService.addAmenity(roomId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
