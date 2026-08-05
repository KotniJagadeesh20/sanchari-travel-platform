package com.travelplatform.hotel.service;

import com.travelplatform.hotel.dto.CreateReviewRequest;
import com.travelplatform.hotel.entity.Hotel;
import com.travelplatform.hotel.entity.HotelReview;
import com.travelplatform.hotel.exception.DuplicateReviewException;
import com.travelplatform.hotel.repository.HotelRepository;
import com.travelplatform.hotel.repository.HotelReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class HotelReviewServiceImpl implements HotelReviewService {

    @Autowired private HotelReviewRepository reviewRepo;
    @Autowired private HotelRepository hotelRepo;
    @Autowired private HotelService hotelService;

    @Override
    @Transactional
    public HotelReview createReview(UUID hotelId, UUID userId, CreateReviewRequest request) {
        Hotel hotel = hotelService.getHotelById(hotelId);

        // Business rule: one review per hotel per user (also enforced by a DB unique
        // constraint on HotelReview — this check just gives a clean 400 instead of a
        // raw constraint-violation 500 in the common case).
        if (reviewRepo.existsByHotelIdAndUserId(hotelId, userId)) {
            throw new DuplicateReviewException(hotelId);
        }

        HotelReview review = new HotelReview();
        review.setHotel(hotel);
        review.setUserId(userId);
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        HotelReview saved = reviewRepo.save(review);

        // Roll the new rating into Hotel's denormalized average incrementally,
        // rather than re-running AVG()/COUNT() over every review on each write.
        int newCount = hotel.getReviewCount() + 1;
        double newAverage = ((hotel.getAverageRating() * hotel.getReviewCount()) + request.getRating()) / newCount;
        hotel.setReviewCount(newCount);
        hotel.setAverageRating(newAverage);
        hotelRepo.save(hotel);

        return saved;
    }

    @Override
    public List<HotelReview> getReviewsByHotel(UUID hotelId) {
        hotelService.getHotelById(hotelId);
        return reviewRepo.findByHotelIdOrderByCreatedAtDesc(hotelId);
    }
}
