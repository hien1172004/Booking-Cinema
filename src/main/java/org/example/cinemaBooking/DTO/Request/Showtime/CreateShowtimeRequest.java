package org.example.cinemaBooking.DTO.Request.Showtime;

import jakarta.validation.constraints.*;
import org.example.cinemaBooking.Shared.contraints.EnumValidator;
import org.example.cinemaBooking.Shared.enums.Language;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request tạo mới một suất chiếu.
 * movieId + roomId được resolve sang entity trong service.
 */
public record CreateShowtimeRequest(

        @NotNull(message = "MOVIE_ID_REQUIRED")
        String movieId,

        @NotNull(message = "ROOM_ID_REQUIRED")
        String roomId,

        @NotNull(message = "START_TIME_REQUIRED")
        @Future(message = "START_TIME_MUST_BE_FUTURE")
        LocalDateTime startTime,

        @NotNull(message = "BASE_PRICE_REQUIRED")
        @DecimalMin(value = "0.0", inclusive = false, message = "BASE_PRICE_MUST_BE_POSITIVE")
        @Digits(integer = 8, fraction = 2, message = "BASE_PRICE_INVALID_FORMAT")
        BigDecimal basePrice,

        @NotNull(message = "LANGUAGE_REQUIRED")
        @EnumValidator(enumClass = Language.class, message = "LANGUAGE_INVALID")
        String language
) {}