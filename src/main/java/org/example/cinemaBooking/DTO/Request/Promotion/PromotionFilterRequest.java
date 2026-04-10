package org.example.cinemaBooking.DTO.Request.Promotion;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PromotionFilterRequest(
        String code,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal minOrderValue,
        BigDecimal maxOrderValue
) {}