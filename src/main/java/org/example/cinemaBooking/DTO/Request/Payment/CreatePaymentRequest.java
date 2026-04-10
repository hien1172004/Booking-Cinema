// CreatePaymentRequest.java
package org.example.cinemaBooking.DTO.Request.Payment;

import jakarta.validation.constraints.NotBlank;

public record CreatePaymentRequest(
    @NotBlank String bookingId,
    String clientIp   // nullable — lấy từ request nếu null
) {}