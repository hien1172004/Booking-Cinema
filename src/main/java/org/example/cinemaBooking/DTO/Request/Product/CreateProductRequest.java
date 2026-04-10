package org.example.cinemaBooking.DTO.Request.Product;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank(message = "NAME_REQUIRED")
        @Size(max = 255)
        String name,

        @NotNull(message = "PRICE_REQUIRED")
        @DecimalMin(value = "0.0", inclusive = false, message = "PRICE_INVALID")
        @Digits(integer = 8, fraction = 2, message = "PRICE_INVALID_FORMAT")
        BigDecimal price,

        @Size(max = 1000)
        @NotBlank(message = "IMAGE_URL_REQUIRED")
        String image
) {}