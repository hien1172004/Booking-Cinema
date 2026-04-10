package org.example.cinemaBooking.DTO.Response.Review;


import java.time.LocalDateTime;

public record ReviewResponse(String id,
                             String movieId,
                             String movieTitle,
                             String userId,
                             Integer rating,
                             String comment,
                             String username,
                             LocalDateTime createdAt,
                             LocalDateTime updatedAt,
                             boolean deleted) {
}
