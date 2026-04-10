// LockSeatRequest.java
package org.example.cinemaBooking.DTO.Request.Seat;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request lock (giữ chỗ tạm) nhiều ghế cùng lúc.
 * userId được lấy từ SecurityContext — KHÔNG nhận từ client.
 */
public record LockSeatRequest(

        @NotEmpty(message = "Danh sách ghế không được rỗng")
        @Size(min = 1, max = 10, message = "Tối đa 10 ghế mỗi lần")
        List<@NotNull String> seatIds
) {}