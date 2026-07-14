package com.travelplatform.hotel.dto;

import com.travelplatform.hotel.entity.HotelReview;

import java.time.LocalDateTime;
import java.util.UUID;

public class ReviewResponse {

    private UUID id;
    private UUID hotelId;
    private UUID userId;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;

    public static ReviewResponse from(HotelReview review) {
        ReviewResponse r = new ReviewResponse();
        r.id = review.getId();
        r.hotelId = review.getHotel().getId();
        r.userId = review.getUserId();
        r.rating = review.getRating();
        r.comment = review.getComment();
        r.createdAt = review.getCreatedAt();
        return r;
    }

    public UUID getId() { return id; }
    public UUID getHotelId() { return hotelId; }
    public UUID getUserId() { return userId; }
    public Integer getRating() { return rating; }
    public String getComment() { return comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
