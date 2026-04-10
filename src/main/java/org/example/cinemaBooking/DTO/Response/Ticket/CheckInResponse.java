// CheckInResponse.java
package org.example.cinemaBooking.DTO.Response.Ticket;


import java.time.LocalDateTime;
import java.util.List;

// CheckInResponse.java
public record CheckInResponse(
        String bookingCode,
        String movieTitle,
        String roomName,
        LocalDateTime showtimeAt,
        List<TicketInfo> tickets,     // danh sách ghế
        List<BookingProductInfo> products     // sản phẩm — chỉ 1 lần
) {
}