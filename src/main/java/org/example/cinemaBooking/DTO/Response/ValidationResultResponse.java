package org.example.cinemaBooking.DTO.Response;

import lombok.Builder;

import java.math.BigDecimal;
@Builder
public record ValidationResultResponse(
        boolean valid,
        BigDecimal discountAmount,
        BigDecimal finalAmount,
        String promotionCode,
        String promotionName
) {
}
