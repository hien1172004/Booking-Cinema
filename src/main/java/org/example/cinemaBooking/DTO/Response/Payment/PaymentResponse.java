// PaymentResponse.java
package org.example.cinemaBooking.DTO.Response.Payment;

import org.example.cinemaBooking.Shared.enums.PaymentMethod;
import org.example.cinemaBooking.Shared.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
    String        paymentId,
    String        bookingId,
    String        bookingCode,
    PaymentMethod paymentMethod,
    PaymentStatus status,
    BigDecimal    amount,
    String        transactionId,
    String        paymentUrl,        // null nếu đã xử lý xong
    LocalDateTime createdAt
) {}