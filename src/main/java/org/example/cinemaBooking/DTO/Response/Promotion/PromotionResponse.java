
package org.example.cinemaBooking.DTO.Response.Promotion;

import org.example.cinemaBooking.Shared.enums.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PromotionResponse(
        String id,
        String code,
        String name,
        String description,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal minOrderValue,
        BigDecimal maxDiscount,
        Integer quantity,
        Integer usedQuantity,
        LocalDate startDate,
        LocalDate endDate,
        Integer maxUsagePerUser,
        boolean active
) {}