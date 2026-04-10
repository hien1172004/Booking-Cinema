package org.example.cinemaBooking.DTO.Response.Cinema;

import org.example.cinemaBooking.Shared.enums.Status;

public record CinemaResponse(
        String id,
        String name,
        String address,
        String phone,
        String hotline,
        String logoUrl,
        Status status
) {}