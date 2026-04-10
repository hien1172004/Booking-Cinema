package org.example.cinemaBooking.DTO.Response;

import lombok.Builder;

@Builder
public record CategoryResponse(
        String id,
        String name
) {
}