package org.example.cinemaBooking.DTO.Request.Room;

import org.example.cinemaBooking.Shared.enums.RoomType;

public record UpdateRoomRequest(
        String name,
        RoomType roomType,
        Integer totalSeats) {
}
