// UnlockSeatRequest.java
package org.example.cinemaBooking.DTO.Request.Seat;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Dùng khi user huỷ giữ chỗ thủ công (thoát màn hình chọn ghế).
 */
public record UnlockSeatRequest(

        @NotEmpty(message = "Danh sách ghế không được rỗng")
        List<@NotNull String> seatIds
) {}