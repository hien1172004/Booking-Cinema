package org.example.cinemaBooking.DTO.Response.Movie;

import org.example.cinemaBooking.Shared.enums.MovieRole;

public record MovieCastResponse(
        String peopleId,
        String name,
        String avatarUrl,
        MovieRole movieRole
) {}