package com.travelplatform.hotel.service;

import com.travelplatform.hotel.dto.AddAmenityRequest;
import com.travelplatform.hotel.dto.AddImageRequest;
import com.travelplatform.hotel.dto.CreateRoomRequest;
import com.travelplatform.hotel.dto.UpdateRoomRequest;
import com.travelplatform.hotel.entity.Hotel;
import com.travelplatform.hotel.entity.Room;
import com.travelplatform.hotel.entity.RoomAmenity;
import com.travelplatform.hotel.entity.RoomImage;
import com.travelplatform.hotel.exception.RoomNotFoundException;
import com.travelplatform.hotel.repository.RoomAmenityRepository;
import com.travelplatform.hotel.repository.RoomImageRepository;
import com.travelplatform.hotel.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class RoomServiceImpl implements RoomService {

    @Autowired private RoomRepository roomRepo;
    @Autowired private RoomImageRepository roomImageRepo;
    @Autowired private RoomAmenityRepository roomAmenityRepo;
    @Autowired private HotelService hotelService;

    @Override
    @Transactional
    public Room addRoom(UUID hotelId, CreateRoomRequest request) {
        Hotel hotel = hotelService.getHotelById(hotelId);

        Room room = new Room();
        room.setHotel(hotel);
        room.setRoomNumber(request.getRoomNumber());
        room.setRoomType(request.getRoomType());
        room.setCapacity(request.getCapacity());
        room.setPricePerNight(request.getPricePerNight());
        room.setTotalRooms(request.getTotalRooms());
        // A freshly created room type starts with its whole inventory available.
        room.setAvailableRooms(request.getTotalRooms());
        room.setDescription(request.getDescription());

        return roomRepo.save(room);
    }

    @Override
    @Transactional
    public Room updateRoom(UUID roomId, UpdateRoomRequest request) {
        Room room = getRoomById(roomId);

        if (request.getRoomNumber() != null) room.setRoomNumber(request.getRoomNumber());
        if (request.getCapacity() != null) room.setCapacity(request.getCapacity());
        if (request.getPricePerNight() != null) room.setPricePerNight(request.getPricePerNight());
        if (request.getDescription() != null) room.setDescription(request.getDescription());
        if (request.getActive() != null) room.setActive(request.getActive());

        if (request.getTotalRooms() != null && !request.getTotalRooms().equals(room.getTotalRooms())) {
            // Shift availableRooms by the same delta so rooms currently held by active
            // bookings stay held: availableRooms = availableRooms + (newTotal - oldTotal),
            // floored at zero in case inventory shrinks below what's already booked out.
            int delta = request.getTotalRooms() - room.getTotalRooms();
            room.setTotalRooms(request.getTotalRooms());
            room.setAvailableRooms(Math.max(0, room.getAvailableRooms() + delta));
        }

        return roomRepo.save(room);
    }

    @Override
    @Transactional
    public void deleteRoom(UUID roomId) {
        Room room = getRoomById(roomId);
        // Soft delete — same reasoning as Hotel: existing bookings hold a FK to this room.
        room.setActive(false);
        roomRepo.save(room);
    }

    @Override
    public List<Room> getRoomsByHotel(UUID hotelId) {
        hotelService.getHotelById(hotelId); // 404s if the hotel itself doesn't exist
        return roomRepo.findByHotelIdAndActiveTrue(hotelId);
    }

    @Override
    public Room getRoomById(UUID roomId) {
        return roomRepo.findById(roomId).orElseThrow(() -> new RoomNotFoundException(roomId));
    }

    @Override
    @Transactional
    public void addImage(UUID roomId, AddImageRequest request) {
        Room room = getRoomById(roomId);
        RoomImage image = new RoomImage();
        image.setRoom(room);
        image.setImageUrl(request.getImageUrl());
        roomImageRepo.save(image);
    }

    @Override
    @Transactional
    public void addAmenity(UUID roomId, AddAmenityRequest request) {
        Room room = getRoomById(roomId);
        RoomAmenity amenity = new RoomAmenity();
        amenity.setRoom(room);
        amenity.setName(request.getName());
        roomAmenityRepo.save(amenity);
    }
}
