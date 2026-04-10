package org.example.cinemaBooking.DTO.Response.Room;

import org.example.cinemaBooking.Shared.enums.RoomType;
import org.example.cinemaBooking.Shared.enums.Status;

import java.time.LocalDateTime;

public record RoomResponse(

        String id,
        String name,
        Integer totalSeats,
        RoomType roomType,
        Status status,

        // cinema info (không trả full object)
        String cinemaId,
        String cinemaName,

        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {}