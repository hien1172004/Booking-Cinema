package org.example.cinemaBooking.DTO.Request.Product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateProductRequest(
        @Size(max = 255)
        String name,

        @DecimalMin(value = "0.0", inclusive = false, message = "PRICE_INVALID")
        @Digits(integer = 8, fraction = 2, message = "PRICE_INVALID_FORMAT")
        BigDecimal price,

        @Size(max = 1000)
        String image,

        Boolean active
) {}