package org.example.cinemaBooking.DTO.Request.Seat;

public record CreateSeatRequest(
        String seatRow,
        Integer seatNumber,
        String roomId,
        String seatTypeId
) {}