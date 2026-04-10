package org.example.cinemaBooking.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.cinemaBooking.DTO.Request.Seat.BulkSeatRequest;
import org.example.cinemaBooking.DTO.Request.Seat.CreateSeatRequest;
import org.example.cinemaBooking.DTO.Request.Seat.UpdateSeatRequest;
import org.example.cinemaBooking.DTO.Response.Seat.SeatResponse;
import org.example.cinemaBooking.Service.Seat.SeatService;
import org.example.cinemaBooking.Shared.constant.ApiPaths;
import org.example.cinemaBooking.Shared.response.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping(ApiPaths.API_V1 + ApiPaths.Seat.BASE)
@Tag(name = "Seat", description = "quản lý ghế")
public class SeatController {

    SeatService seatService;

    @Operation(summary = "Tạo ghế mới",
            description = "Tạo một ghế mới. Yêu cầu quyền ADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    ApiResponse<SeatResponse> createSeat(
            @Valid @RequestBody CreateSeatRequest request
    ) {
        var response = seatService.createSeat(request);
        return ApiResponse.<SeatResponse>builder()
                .success(true)
                .message("Seat created successfully")
                .data(response)
                .build();
    }


    @Operation(summary = "Tạo nhiều ghế",
            description = "Tạo nhiều ghế trong một phòng chiếu. Yêu cầu quyền ADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/rooms/{roomId}/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    ApiResponse<List<SeatResponse>> createSeatBulk(
            @PathVariable String roomId,
            @Valid @RequestBody BulkSeatRequest request
    ) {
        var response = seatService.createBulkSeats(roomId, request);
        return ApiResponse.<List<SeatResponse>>builder()
                .success(true)
                .message("Seats created successfully")
                .data(response)
                .build();
    }

    @Operation(summary = "Lấy thông tin ghế",
            description = "Lấy thông tin ghế theo ID.")
    @GetMapping("/{seatId}")
    ApiResponse<SeatResponse> getSeatById(@PathVariable String seatId) {
        var response = seatService.getSeat(seatId);
        return ApiResponse.<SeatResponse>builder()
                .success(true)
                .message("Seat retrieved successfully")
                .data(response)
                .build();
    }

    @Operation(summary = "Lấy danh sách ghế theo phòng chiếu",
            description = "Lấy danh sách tất cả ghế trong một phòng chiếu.")
    @GetMapping("/rooms/{roomId}")
    ApiResponse<List<SeatResponse>> getSeatsByRoomId(@PathVariable String roomId) {
        var response = seatService.getSeatsByRoom(roomId);
        return ApiResponse.<List<SeatResponse>>builder()
                .success(true)
                .message("Seats retrieved successfully")
                .data(response)
                .build();
    }

    @Operation(summary = "Cập nhật thông tin ghế",
            description = "Cập nhật thông tin ghế theo ID. Yêu cầu quyền ADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{seatId}")
    @PreAuthorize("hasRole('ADMIN')")
    ApiResponse<SeatResponse> updateSeat(
            @PathVariable String seatId,
            @Valid @RequestBody UpdateSeatRequest request
    ) {
        var response = seatService.updateSeat(seatId, request);
        return ApiResponse.<SeatResponse>builder()
                .success(true)
                .message("Seat updated successfully")
                .data(response)
                .build();
    }

    @Operation(summary = "Xóa ghế",
            description = "Xóa ghế theo ID. Yêu cầu quyền ADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{seatId}")
    @PreAuthorize("hasRole('ADMIN')")
    ApiResponse<Void> deleteSeat(@PathVariable String seatId) {
        seatService.deleteSeat(seatId);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Seat deleted successfully")
                .build();
    }
}
