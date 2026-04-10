package org.example.cinemaBooking.DTO.Request;

import jakarta.validation.constraints.NotBlank;

public record CategoryRequest(
        @NotBlank(message = "CATEGORY_NAME_REQUIRED")
        String name
) {
}