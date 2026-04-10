package org.example.cinemaBooking.DTO.Request.Cinema;

import jakarta.validation.constraints.NotBlank;

public record CreateCinemaRequest(
        @NotBlank(message = "NAME_REQUIRED") String name,
        @NotBlank(message = "ADDRESS_REQUIRED") String address,
        @NotBlank(message = "PHONE_REQUIRED") String phone,
        @NotBlank(message = "HOTLINE_REQUIRED")  String hotline,
        @NotBlank(message = "LOGO_URL_REQUIRED") String logoUrl
) {}