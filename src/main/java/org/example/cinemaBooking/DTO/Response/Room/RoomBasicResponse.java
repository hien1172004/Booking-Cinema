package org.example.cinemaBooking.DTO.Response.Room;

import lombok.Builder;
import org.example.cinemaBooking.Shared.enums.RoomType;
import org.example.cinemaBooking.Shared.enums.Status;

import java.time.LocalDateTime;
@Builder
public record RoomBasicResponse(
        String id,
        String name,
        Integer totalSeats,
        RoomType roomType,
        Status status,
        LocalDateTime createdAt
) {}