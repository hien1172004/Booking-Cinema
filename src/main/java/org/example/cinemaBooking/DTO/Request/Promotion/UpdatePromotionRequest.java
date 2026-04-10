package org.example.cinemaBooking.DTO.Request.Promotion;

import jakarta.validation.constraints.*;
import org.example.cinemaBooking.Shared.enums.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdatePromotionRequest(

        @Size(max = 50, message = "NAME_TOO_LONG")
        String name,

        @Size(max = 1000, message = "DESCRIPTION_TOO_LONG")
        String description,

        DiscountType discountType,

        @DecimalMin(value = "0.0", inclusive = false, message = "DISCOUNT_VALUE_INVALID")
        @Digits(integer = 10, fraction = 2, message = "DISCOUNT_VALUE_FORMAT_INVALID")
        BigDecimal discountValue,

        @DecimalMin(value = "0.0", inclusive = true, message = "MIN_ORDER_VALUE_INVALID")
        @Digits(integer = 10, fraction = 2, message = "MIN_ORDER_VALUE_FORMAT_INVALID")
        BigDecimal minOrderValue,

        @DecimalMin(value = "0.0", inclusive = true, message = "MAX_DISCOUNT_INVALID")
        @Digits(integer = 10, fraction = 2, message = "MAX_DISCOUNT_FORMAT_INVALID")
        BigDecimal maxDiscount,

        @Min(value = 1, message = "QUANTITY_INVALID")
        Integer quantity,

        LocalDate startDate,
        LocalDate endDate,

        @Min(value = 1, message = "MAX_USAGE_PER_USER_INVALID")
        Integer maxUsagePerUser

) {}