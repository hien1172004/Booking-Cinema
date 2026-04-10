// ShowtimeSeatResponse.java
package org.example.cinemaBooking.DTO.Response.Showtime;

import org.example.cinemaBooking.Shared.enums.SeatStatus;
import org.example.cinemaBooking.Shared.enums.SeatTypeEnum;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Thông tin 1 ghế trong suất chiếu.
 */
public record ShowtimeSeatResponse(
        String showtimeSeatId,
        String seatId,
        String seatRow,
        Integer seatNumber,
        SeatTypeEnum seatType,
        BigDecimal finalPrice,        // basePrice + priceModifier
        SeatStatus status,
        LocalDateTime lockedUntil,    // null nếu không bị lock
        String lockedByUser             // null nếu không bị lock
) {}