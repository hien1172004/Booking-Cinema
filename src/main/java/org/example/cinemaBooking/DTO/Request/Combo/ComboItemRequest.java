package org.example.cinemaBooking.DTO.Request.Combo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ComboItemRequest(
        @NotBlank(message = "PRODUCT_ID_REQUIRED") String productId,
        @Min(value = 1, message = "QUANTITY_MIN_VALUE") Integer quantity
) {}