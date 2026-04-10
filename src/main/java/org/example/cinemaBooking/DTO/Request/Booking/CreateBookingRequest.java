package org.example.cinemaBooking.DTO.Request.Booking;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.example.cinemaBooking.Shared.enums.ItemType;


import java.util.List;

// CreateBookingRequest.java
public record CreateBookingRequest(

    @NotEmpty(message = "Phải chọn ít nhất 1 ghế")
    @Size(max = 10)
    List<@NotBlank String> seatIds,

    @NotBlank(message = "Showtime không được trống")
    String showtimeId,

    /** nullable — không bắt buộc dùng promotion */
    String promotionCode,

    /** Đồ ăn/combo đi kèm — có thể rỗng */

    List<@Valid BookingProductItem> products
) {
    public record BookingProductItem(
            @NotBlank String itemId,
            @NotNull ItemType itemType,   // PRODUCT | COMBO
            @Min(1)   Integer quantity
    ) {}

}