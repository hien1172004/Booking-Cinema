package org.example.cinemaBooking.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.cinemaBooking.DTO.Request.Ticket.CheckInRequest;
import org.example.cinemaBooking.DTO.Response.Ticket.CheckInResponse;
import org.example.cinemaBooking.DTO.Response.Ticket.TicketResponse;
import org.example.cinemaBooking.Service.Ticket.TicketService;
import org.example.cinemaBooking.Shared.constant.ApiPaths;
import org.example.cinemaBooking.Shared.response.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping(ApiPaths.API_V1 + ApiPaths.Ticket.BASE)
@Tag(name = "Ticket", description = "quản lý vé và QR")
public class TicketController {
    TicketService ticketService;

    /** Xem tất cả vé của mình */
    @Operation(summary = "Lấy danh sách vé của tôi",
            description = "Trả về tất cả vé mà người dùng hiện tại đã mua hoặc sở hữu.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<TicketResponse>> getMyTickets() {
        return ApiResponse.<List<TicketResponse>>builder()
                .success(true)
                .message("Lấy vé thành công")
                .data(ticketService.getMyTickets())
                .build();
    }

    /** Xem vé theo booking */
    @Operation(summary = "Lấy vé theo booking",
            description = "Trả về danh sách vé liên quan đến bookingId được cung cấp.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/booking/{bookingId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<TicketResponse>> getTicketsByBooking(
            @PathVariable String bookingId) {
        return ApiResponse.<List<TicketResponse>>builder()
                .success(true)
                .message("Lấy vé theo đặt vé thành công")
                .data(ticketService.getTicketsByBooking(bookingId))
                .build();
    }

    /** Lấy QR code — base64 PNG */
    @Operation(summary = "Lấy QR code (base64)",
            description = "Lấy QR code dưới dạng chuỗi base64 PNG cho booking code. Sử dụng để hiển thị hoặc gửi qua email.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{bookingCode}/qr")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<String> getQRCode(
            @PathVariable String bookingCode) {
        return ApiResponse.<String>builder()
                .success(true)
                .message("Lấy QR code thành công")
                .data(ticketService.getBookingQR(bookingCode))
                .build();
    }


    @Operation(summary = "Check-in nhiều vé",
            description = "Thực hiện check-in nhiều vé theo booking code (dành cho nhân viên).",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/check-in")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ApiResponse<CheckInResponse> checkInMultiple(
            @Valid @RequestBody CheckInRequest requests) {
        return ApiResponse.<CheckInResponse>builder()
                .success(true)
                .message("Check-in thành công")
                .data(ticketService.checkInByBookingCode(requests.bookingCode()))
                .build();
    }
}
