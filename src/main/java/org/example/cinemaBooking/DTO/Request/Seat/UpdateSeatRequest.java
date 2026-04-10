package org.example.cinemaBooking.DTO.Request.Seat;

public record UpdateSeatRequest(
        String seatRow,
        Integer seatNumber,
        Boolean isActive,
        String seatTypeId,
        boolean active) {}