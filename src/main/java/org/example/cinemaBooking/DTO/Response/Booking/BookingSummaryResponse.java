package org.example.cinemaBooking.DTO.Response.Booking;

import org.example.cinemaBooking.Shared.enums.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// BookingSummaryResponse.java — dùng cho danh sách
public record BookingSummaryResponse(
    String bookingId,
    String bookingCode,
    String movieTitle,
    LocalDateTime startTime,
    int seatCount,
    BigDecimal finalPrice,
    BookingStatus status,
    LocalDateTime createdAt
) {}