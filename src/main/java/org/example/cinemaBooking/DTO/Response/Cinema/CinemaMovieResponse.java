package org.example.cinemaBooking.DTO.Response.Cinema;

import lombok.Builder;
import org.example.cinemaBooking.Shared.enums.Language;
@Builder
public record CinemaMovieResponse(
        String movieId,
        String movieTitle,
        String posterUrl,
        int durationMinutes,
        Language language
) {}