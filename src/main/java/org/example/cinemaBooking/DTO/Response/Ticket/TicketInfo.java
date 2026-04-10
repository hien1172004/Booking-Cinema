package org.example.cinemaBooking.DTO.Response.Ticket;

import org.example.cinemaBooking.Shared.enums.SeatTypeEnum;
import org.example.cinemaBooking.Shared.enums.TicketStatus;

import java.time.LocalDateTime;

// TicketInfo.java
public record TicketInfo(
        String ticketCode,
        String seatRow,
        Integer seatNumber,
        SeatTypeEnum seatType,
        TicketStatus status,
        LocalDateTime checkedInAt
) {
}