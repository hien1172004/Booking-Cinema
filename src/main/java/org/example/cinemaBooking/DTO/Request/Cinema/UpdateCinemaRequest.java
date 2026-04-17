package org.example.cinemaBooking.DTO.Request.Cinema;

import lombok.Builder;

@Builder
public record UpdateCinemaRequest(
        String name,
        String address,
        String phone,
        String hotline,
        String logoUrl
) {}