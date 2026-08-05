package com.travelplatform.hotel.service;

import com.travelplatform.hotel.dto.AddAmenityRequest;
import com.travelplatform.hotel.dto.AddImageRequest;
import com.travelplatform.hotel.dto.CreateRoomRequest;
import com.travelplatform.hotel.dto.UpdateRoomRequest;
import com.travelplatform.hotel.entity.Room;

import java.util.List;
import java.util.UUID;

public interface RoomService {

    Room addRoom(UUID hotelId, CreateRoomRequest request);

    Room updateRoom(UUID roomId, UpdateRoomRequest request);

    void deleteRoom(UUID roomId);

    List<Room> getRoomsByHotel(UUID hotelId);

    Room getRoomById(UUID roomId);

    void addImage(UUID roomId, AddImageRequest request);

    void addAmenity(UUID roomId, AddAmenityRequest request);
}
