// SeatMapResponse.java
package org.example.cinemaBooking.DTO.Response.Showtime;

import java.util.List;
import java.util.Map;

/**
 * Sơ đồ ghế theo hàng, trả về 1 lần duy nhất cho client render.
 * Map<seatRow, List<ShowtimeSeatResponse>>
 */
public record SeatMapResponse(
        String showtimeId,
        int totalSeats,
        int availableSeats,
        Map<String, List<ShowtimeSeatResponse>> seatMap   // "A" -> [A1, A2, ...]
) {}