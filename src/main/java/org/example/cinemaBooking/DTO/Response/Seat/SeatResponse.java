package org.example.cinemaBooking.DTO.Response.Seat;

import java.math.BigDecimal;

public record SeatResponse(
        String id,
        String seatRow,
        Integer seatNumber,
        Boolean active,
        String  roomId,
        String  seatTypeId,
        String seatTypeName,
        BigDecimal priceModifier
) {}