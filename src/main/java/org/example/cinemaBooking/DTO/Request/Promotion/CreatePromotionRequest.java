package org.example.cinemaBooking.DTO.Request.Promotion;

import jakarta.validation.constraints.*;
import org.example.cinemaBooking.Shared.enums.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreatePromotionRequest(

        @NotBlank(message = "CODE_REQUIRED")
        @Size(max = 50, message = "CODE_TOO_LONG")
        String code,

        @NotBlank(message = "NAME_REQUIRED")
        @Size(max = 50, message = "NAME_TOO_LONG")
        String name,

        @Size(max = 1000, message = "DESCRIPTION_TOO_LONG")
        String description,

        @NotNull(message = "DISCOUNT_TYPE_REQUIRED")
        DiscountType discountType,

        @NotNull(message = "DISCOUNT_VALUE_REQUIRED")
        @DecimalMin(value = "0.0", inclusive = false, message = "DISCOUNT_VALUE_INVALID")
        @Digits(integer = 10, fraction = 2, message = "DISCOUNT_VALUE_FORMAT_INVALID")
        BigDecimal discountValue,

        @DecimalMin(value = "0.0", inclusive = true, message = "MIN_ORDER_VALUE_INVALID")
        @Digits(integer = 10, fraction = 2, message = "MIN_ORDER_VALUE_FORMAT_INVALID")
        BigDecimal minOrderValue,

        @DecimalMin(value = "0.0", inclusive = true, message = "MAX_DISCOUNT_INVALID")
        @Digits(integer = 10, fraction = 2, message = "MAX_DISCOUNT_FORMAT_INVALID")
        BigDecimal maxDiscount,

        @NotNull(message = "QUANTITY_REQUIRED")
        @Min(value = 1, message = "QUANTITY_INVALID")
        Integer quantity,

        @NotNull(message = "START_DATE_REQUIRED")
        LocalDate startDate,

        @NotNull(message = "END_DATE_REQUIRED")
        LocalDate endDate,

        @NotNull(message = "MAX_USAGE_PER_USER_REQUIRED")
        @Min(value = 1, message = "MAX_USAGE_PER_USER_INVALID")
        Integer maxUsagePerUser

) {}