package com.travelplatform.hotel.service;

import com.travelplatform.hotel.dto.AddAmenityRequest;
import com.travelplatform.hotel.dto.AddImageRequest;
import com.travelplatform.hotel.dto.CreateHotelRequest;
import com.travelplatform.hotel.dto.UpdateHotelRequest;
import com.travelplatform.hotel.entity.Hotel;
import com.travelplatform.hotel.entity.HotelAmenity;
import com.travelplatform.hotel.entity.HotelImage;
import com.travelplatform.hotel.enums.RoomType;
import com.travelplatform.hotel.exception.HotelNotFoundException;
import com.travelplatform.hotel.repository.HotelAmenityRepository;
import com.travelplatform.hotel.repository.HotelImageRepository;
import com.travelplatform.hotel.repository.HotelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class HotelServiceImpl implements HotelService {

    @Autowired private HotelRepository hotelRepo;
    @Autowired private HotelImageRepository hotelImageRepo;
    @Autowired private HotelAmenityRepository hotelAmenityRepo;

    @Override
    @Transactional
    public Hotel createHotel(CreateHotelRequest request) {
        Hotel hotel = new Hotel();
        hotel.setName(request.getName());
        hotel.setDescription(request.getDescription());
        hotel.setDestinationId(request.getDestinationId());
        hotel.setAddress(request.getAddress());
        hotel.setCity(request.getCity());
        hotel.setState(request.getState());
        hotel.setCountry(request.getCountry());
        hotel.setLatitude(request.getLatitude());
        hotel.setLongitude(request.getLongitude());
        hotel.setStarRating(request.getStarRating());
        hotel.setContactEmail(request.getContactEmail());
        hotel.setContactPhone(request.getContactPhone());
        hotel.setCheckInTime(request.getCheckInTime());
        hotel.setCheckOutTime(request.getCheckOutTime());
        return hotelRepo.save(hotel);
    }

    @Override
    @Transactional
    public Hotel updateHotel(UUID hotelId, UpdateHotelRequest request) {
        Hotel hotel = getHotelById(hotelId);

        if (request.getName() != null) hotel.setName(request.getName());
        if (request.getDescription() != null) hotel.setDescription(request.getDescription());
        if (request.getAddress() != null) hotel.setAddress(request.getAddress());
        if (request.getCity() != null) hotel.setCity(request.getCity());
        if (request.getState() != null) hotel.setState(request.getState());
        if (request.getCountry() != null) hotel.setCountry(request.getCountry());
        if (request.getLatitude() != null) hotel.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) hotel.setLongitude(request.getLongitude());
        if (request.getStarRating() != null) hotel.setStarRating(request.getStarRating());
        if (request.getContactEmail() != null) hotel.setContactEmail(request.getContactEmail());
        if (request.getContactPhone() != null) hotel.setContactPhone(request.getContactPhone());
        if (request.getCheckInTime() != null) hotel.setCheckInTime(request.getCheckInTime());
        if (request.getCheckOutTime() != null) hotel.setCheckOutTime(request.getCheckOutTime());
        if (request.getActive() != null) hotel.setActive(request.getActive());

        return hotelRepo.save(hotel);
    }

    @Override
    @Transactional
    public void deleteHotel(UUID hotelId) {
        Hotel hotel = getHotelById(hotelId);
        // Soft delete: existing bookings/reviews hold a FK to this hotel, so a hard
        // delete would either cascade-destroy booking history or fail on the FK
        // constraint. Delisting (active=false) keeps history intact and matches the
        // soft-delete pattern already used for packages/destinations on this platform.
        hotel.setActive(false);
        hotelRepo.save(hotel);
    }

    @Override
    public Hotel getHotelById(UUID hotelId) {
        return hotelRepo.findById(hotelId).orElseThrow(() -> new HotelNotFoundException(hotelId));
    }

    @Override
    public Page<Hotel> searchHotels(UUID destinationId, Integer starRating, RoomType roomType,
                                     Double minPrice, Double maxPrice, Pageable pageable) {
        return hotelRepo.search(destinationId, starRating, roomType, minPrice, maxPrice, pageable);
    }

    @Override
    @Transactional
    public void addImage(UUID hotelId, AddImageRequest request) {
        Hotel hotel = getHotelById(hotelId);
        HotelImage image = new HotelImage();
        image.setHotel(hotel);
        image.setImageUrl(request.getImageUrl());
        image.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        hotelImageRepo.save(image);
    }

    @Override
    @Transactional
    public void addAmenity(UUID hotelId, AddAmenityRequest request) {
        Hotel hotel = getHotelById(hotelId);
        HotelAmenity amenity = new HotelAmenity();
        amenity.setHotel(hotel);
        amenity.setName(request.getName());
        amenity.setIcon(request.getIcon());
        hotelAmenityRepo.save(amenity);
    }
}
