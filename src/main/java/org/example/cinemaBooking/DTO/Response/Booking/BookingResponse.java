package org.example.cinemaBooking.DTO.Response.Booking;

import org.example.cinemaBooking.Shared.enums.BookingStatus;
import org.example.cinemaBooking.Shared.enums.ItemType;
import org.example.cinemaBooking.Shared.enums.SeatTypeEnum;
import org.example.cinemaBooking.Shared.enums.TicketStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


public record BookingResponse(
    String bookingId,
    String bookingCode,
    BookingStatus status,
    ShowtimeInfo showtime,
    List<TicketInfo> tickets,
    List<ProductInfo> products,
    BigDecimal totalPrice,
    BigDecimal discountAmount,
    BigDecimal finalPrice,
    LocalDateTime expiredAt,
    String paymentUrl           // null nếu đã confirmed/cancelled

) {
    public record ShowtimeInfo(
        String showtimeId,
        String movieTitle,
        String roomName,
        String cinemaName,
        LocalDateTime startTime
    ) {}

    public record TicketInfo(
        String ticketCode,
        String seatRow,
        Integer seatNumber,
        SeatTypeEnum seatType,
        BigDecimal price,
        TicketStatus status
    ) {}

    public record ProductInfo(
        String itemId,
        String itemName,
        ItemType itemType,
        BigDecimal itemPrice,
        Integer quantity,
        BigDecimal subtotal
    ) {}
}