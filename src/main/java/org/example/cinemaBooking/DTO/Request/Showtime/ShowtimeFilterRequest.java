package org.example.cinemaBooking.DTO.Request.Showtime;

import jakarta.validation.constraints.Min;
import org.example.cinemaBooking.Shared.enums.Language;
import org.example.cinemaBooking.Shared.enums.ShowTimeStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Query params để filter danh sách suất chiếu.
 * Tất cả optional — nếu null thì không áp dụng điều kiện đó.
 */
public record ShowtimeFilterRequest(

        String movieId,
        String cinemaId,
        String roomId,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate date,          // lọc theo ngày chiếu

        Language language,
        ShowTimeStatus status,
        String keyword,

        @Min(0) int page,
        @Min(1) int size        // mặc định sẽ được set trong service nếu = 0
) {
    /** Compact constructor: gán default nếu caller không truyền */
    public ShowtimeFilterRequest {
        if (size <= 0) size = 20;
        if (page < 0)  page = 0;
    }
}