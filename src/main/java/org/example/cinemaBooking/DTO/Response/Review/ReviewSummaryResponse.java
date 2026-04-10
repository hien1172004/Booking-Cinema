package org.example.cinemaBooking.DTO.Response.Review;

import java.time.LocalDateTime;

public record ReviewSummaryResponse(
        String id,
        Integer rating,
        String commentTruncated,    // cắt ngắn nếu dài
        String username,
        LocalDateTime createdAt
//        Double averageRating        // optional: nếu tính trung bình ở service
) {}