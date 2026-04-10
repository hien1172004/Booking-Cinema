// TicketResponse.java
package org.example.cinemaBooking.DTO.Response.Ticket;

import org.example.cinemaBooking.Shared.enums.SeatTypeEnum;
import org.example.cinemaBooking.Shared.enums.TicketStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TicketResponse(
    String        ticketId,
    String        ticketCode,
    TicketStatus  status,
    BigDecimal    price,
    SeatInfo      seat,
    ShowtimeInfo  showtime,
    LocalDateTime checkedInAt
) {
    public record SeatInfo(
        String       seatRow,
        Integer      seatNumber,
        SeatTypeEnum seatType
    ) {}

    public record ShowtimeInfo(
        String        movieTitle,
        String        cinemaName,
        String        roomName,
        LocalDateTime startTime
    ) {}
}