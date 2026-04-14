package org.example.cinemaBooking.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.cinemaBooking.DTO.Request.Booking.CreateBookingRequest;
import org.example.cinemaBooking.DTO.Response.Booking.BookingResponse;
import org.example.cinemaBooking.DTO.Response.Booking.BookingSummaryResponse;
import org.example.cinemaBooking.Service.Booking.BookingService;
import org.example.cinemaBooking.Shared.constant.ApiPaths;
import org.example.cinemaBooking.Shared.constraints.RateLimit;
import org.example.cinemaBooking.Shared.response.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "Booking", description = "quản lý đặt vé")
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping(ApiPaths.API_V1 + ApiPaths.Booking.BASE)
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class BookingController {
    BookingService bookingService;

    @Operation(summary = "Tạo đặt vé mới",
            description = "Tạo một đặt vé mới cho một suất chiếu phim")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @RateLimit(capacity = 5, refillPerMinute = 5)
    public ApiResponse<BookingResponse> createBooking(@Valid @RequestBody CreateBookingRequest request) {
        var response = bookingService.createBooking(request);
        log.info("[BookingController] createBooking - Booking created with code: {}", response.bookingCode());
        return ApiResponse.<BookingResponse>builder()
                .data(response)
                .success(true)
                .message("Tạo đặt vé thành công")
                .build();
    }

    @Operation(summary = "Lấy chi tiết đặt vé",
            description = "Lấy chi tiết của một đặt vé cụ thể theo ID")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{bookingId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<BookingResponse> getBookingById(@PathVariable String bookingId) {
        var response = bookingService.getBookingById(bookingId);
        log.info("[BookingController] getBookingById - Retrieved booking with code: {}", response.bookingCode());
        return ApiResponse.<BookingResponse>builder()
                .data(response)
                .success(true)
                .message("Lấy đặt vé thành công")
                .build();
    }

    @Operation(summary = "Lấy đặt vé của tôi",
            description = "Lấy danh sách các đặt vé do người dùng hiện tại đã thực hiện")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<BookingSummaryResponse>> getMyBookings() {
        var response = bookingService.getMyBookings();
        log.info("[BookingController] getMyBookings - Retrieved {} bookings for current user", response.size());
        return ApiResponse.<List<BookingSummaryResponse>>builder()
                .data(response)
                .success(true)
                .message("Lấy đặt vé của tôi thành công")
                .build();
    }

    @Operation(summary = "Hủy đặt vé",
            description = "Hủy một đặt vé hiện có theo ID")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{bookingId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<BookingResponse> cancelBooking(@PathVariable String bookingId) {
        var response = bookingService.cancelBooking(bookingId);
        log.info("[BookingController] cancelBooking - Canceled booking with ID: {}", bookingId);
        return ApiResponse.<BookingResponse>builder()
                .success(true)
                .data(response)
                .message("Hủy đặt vé thành công")
                .build();
    }

}
