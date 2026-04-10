package org.example.cinemaBooking.DTO.Request.Seat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BulkSeatRequest(
        @NotEmpty(message = "Seat groups cannot be empty")
        @Valid
        List<SeatGroup> seatGroups
) {
    public record SeatGroup(
            @NotEmpty(message = "Rows cannot be empty")
            List<String> rows,

            @NotEmpty(message = "Numbers cannot be empty")
            List<Integer> numbers,

            @NotBlank(message = "Seat type ID is required")
            String seatTypeId
    ) {}
}