package org.example.cinemaBooking.DTO.Response.Showtime;

import org.example.cinemaBooking.Shared.enums.Language;
import org.example.cinemaBooking.Shared.enums.ShowTimeStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lightweight response dùng cho danh sách suất chiếu
 * (lịch chiếu theo phim, theo rạp, theo ngày …).
 */
public record ShowtimeSummaryResponse(

        String id,

        // ── Movie info ──────────────────────────────────────────────
        String movieId,
        String movieTitle,
        String posterUrl,
        int durationMinutes,

        // ── Room / Cinema info ──────────────────────────────────────
        String roomId,
        String roomName,
        String cinemaId,
        String cinemaName,

        // ── Showtime info ───────────────────────────────────────────
        LocalDateTime startTime,
        LocalDateTime endTime,      // computed via mapper expression
        BigDecimal basePrice,
        Language language,
        ShowTimeStatus status,
        int availableSeats,
        boolean bookable       // computed
) {
}