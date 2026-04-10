package org.example.cinemaBooking.DTO.Request.Combo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CreateComboRequest(
        @NotBlank(message = "NAME_REQUIRED")
        String name,

        @NotNull(message = "PRICE_REQUIRED")
        @DecimalMin(value = "0.0", inclusive = false, message = "PRICE_INVALID")
        @Digits(integer = 8, fraction = 2)
        BigDecimal price,

        @NotBlank(message = "IMAGE_URL_REQUIRED")
        String image,

        String description,

        @NotNull(message = "ITEMS_REQUIRED")
        List<ComboItemRequest> items
) {}