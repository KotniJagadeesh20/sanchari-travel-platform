package com.travelplatform.hotel.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.travelplatform.hotel.dto.*;
import com.travelplatform.hotel.entity.Hotel;
import com.travelplatform.hotel.entity.Room;
import com.travelplatform.hotel.service.HotelService;
import com.travelplatform.hotel.service.RoomService;

/**
 * Admin-only hotel and room management. Enforcement of ROLE_ADMIN happens in
 * SecurityConfig (path-based), not per-method here — keeps controllers focused on
 * request/response shaping.
 */
@RestController
@RequestMapping("/hotels/admin")
@Validated
@Tag(name = "Hotel Admin", description = "Create/update/delete hotels, rooms, images, amenities — requires ROLE_ADMIN")
@SecurityRequirement(name = "bearerAuth")
public class HotelAdminController {

    @Autowired private HotelService hotelService;
    @Autowired private RoomService roomService;

    @Operation(summary = "Create a hotel")
    @PostMapping
    public ResponseEntity<HotelResponse> createHotel(@Validated @RequestBody CreateHotelRequest request) {
        Hotel hotel = hotelService.createHotel(request);
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
