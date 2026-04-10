package org.example.cinemaBooking.DTO.Request.Showtime;

import jakarta.validation.constraints.*;
import org.example.cinemaBooking.Shared.contraints.EnumValidator;
import org.example.cinemaBooking.Shared.enums.Language;
import org.example.cinemaBooking.Shared.enums.ShowTimeStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request cập nhật suất chiếu (partial — chỉ các trường không null mới được apply).
 * MapStruct dùng NullValuePropertyMappingStrategy.IGNORE để bỏ qua null.
 */
public record UpdateShowtimeRequest(

        // Cho phép đổi phòng / giờ khi suất vẫn còn SCHEDULED
        String roomId,

        @Future(message = "START_TIME_MUST_BE_IN_FUTURE")
        LocalDateTime startTime,

        @DecimalMin(value = "0.0", inclusive = false, message = "BASE_PRICE_MUST_BE_POSITIVE")
        @Digits(integer = 8, fraction = 2)
        BigDecimal basePrice,

        @EnumValidator(enumClass = Language.class, message = "LANGUAGE_INVALID")
        String language,

        // Admin có thể chủ động set CANCELLED
        @EnumValidator(enumClass = ShowTimeStatus.class, message = "SHOWTIME_STATUS_INVALID")
        String status
) {}