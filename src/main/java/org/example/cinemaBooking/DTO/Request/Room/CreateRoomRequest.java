package org.example.cinemaBooking.DTO.Request.Room;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.cinemaBooking.Shared.contraints.EnumValidator;
import org.example.cinemaBooking.Shared.enums.RoomType;

public record CreateRoomRequest(

        @NotBlank(message = "NAME_REQUIRED")
        String name,

        @NotNull(message = "TOTAL_SEATS_REQUIRED")
        Integer totalSeats,

        @EnumValidator(enumClass = RoomType.class, message = "ROOM_TYPE_INVALID")
        String roomType,

        @NotBlank(message = "CINEMA_ID_REQUIRED")
        String cinemaId

) {}