package org.example.cinemaBooking.DTO.Response.Movie;

public record MovieRecommendResponse(
        String id,
        String title,
        String posterUrl,
        Double score
) {}