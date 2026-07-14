package com.travelplatform.hotel.service;

import com.travelplatform.hotel.dto.AddAmenityRequest;
import com.travelplatform.hotel.dto.AddImageRequest;
import com.travelplatform.hotel.dto.CreateHotelRequest;
import com.travelplatform.hotel.dto.UpdateHotelRequest;
import com.travelplatform.hotel.entity.Hotel;
import com.travelplatform.hotel.enums.RoomType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface HotelService {

    Hotel createHotel(CreateHotelRequest request);

    Hotel updateHotel(UUID hotelId, UpdateHotelRequest request);

    void deleteHotel(UUID hotelId);

    Hotel getHotelById(UUID hotelId);

    Page<Hotel> searchHotels(UUID destinationId, Integer starRating, RoomType roomType,
                              Double minPrice, Double maxPrice, Pageable pageable);

    void addImage(UUID hotelId, AddImageRequest request);

    void addAmenity(UUID hotelId, AddAmenityRequest request);
}
