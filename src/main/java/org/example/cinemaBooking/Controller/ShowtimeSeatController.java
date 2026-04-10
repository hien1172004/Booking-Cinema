package org.example.cinemaBooking.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.cinemaBooking.DTO.Request.Seat.LockSeatRequest;
import org.example.cinemaBooking.DTO.Request.Seat.UnlockSeatRequest;
import org.example.cinemaBooking.DTO.Response.Showtime.SeatMapResponse;
import org.example.cinemaBooking.DTO.Response.Showtime.ShowtimeSeatResponse;
import org.example.cinemaBooking.Service.Showtime.ShowTimeSeatService;
import org.example.cinemaBooking.Shared.constant.ApiPaths;
import org.example.cinemaBooking.Shared.response.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping(ApiPaths.API_V1 + ApiPaths.Showtime.BASE + "/{showtimeId}" + ApiPaths.Seat.BASE)
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
@Tag(name = "ShowtimeSeat", description = "quản lý ghế cho suất chiếu (sơ đồ, giữ/huỷ giữ)")
public class ShowtimeSeatController {

    ShowTimeSeatService showTImeSeatService;

    @Operation(summary = "Lấy sơ đồ ghế",
            description = "Lấy sơ đồ ghế cho suất chiếu (bao gồm trạng thái từng ghế).")
    @GetMapping
    ApiResponse<SeatMapResponse> getSeatMap(@PathVariable String showtimeId) {
        SeatMapResponse response = showTImeSeatService.getSeatMap(showtimeId);
        return ApiResponse.<SeatMapResponse>builder()
                .success(true)
                .data(response)
                .message("Lấy sơ đồ ghế thành công")
                .build();
    }

    @Operation(summary = "Lấy ghế đang giữ của tôi",
            description = "Lấy danh sách ghế mà người dùng hiện tại đang giữ cho suất chiếu.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/my-locked-seats")
    @PreAuthorize("isAuthenticated()")
    ApiResponse<List<ShowtimeSeatResponse>> getMyLockedSeats(@PathVariable String showtimeId){
        var response = showTImeSeatService.getMyLockedSeats(showtimeId);
        return ApiResponse.<List<ShowtimeSeatResponse>>builder()
                .success(true)
                .data(response)
                .message("Lấy danh sách ghế đang giữ thành công")
                .build();
    }

    @Operation(summary = "Giữ ghế",
            description = "Giữ danh sách ghế cho suất chiếu (tạm thời).",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/lock")
    @PreAuthorize("isAuthenticated()")
    ApiResponse<List<ShowtimeSeatResponse>> lockSeats(@PathVariable String showtimeId,
                                                      @RequestBody @Valid LockSeatRequest lockSeatRequest){
        var response = showTImeSeatService.lockSeats(showtimeId, lockSeatRequest);
        return ApiResponse.<List<ShowtimeSeatResponse>>builder()
                .success(true)
                .data(response)
                .message("Giữ chỗ thành công")
                .build();
    }

    @Operation(summary = "Huỷ giữ ghế",
            description = "Huỷ giữ các ghế đã được giữ trước đó.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/unlock")
    @PreAuthorize("isAuthenticated()")
    ApiResponse<List<ShowtimeSeatResponse>> unlockSeats(@PathVariable String showtimeId,
                                 @RequestBody @Valid UnlockSeatRequest unlockSeatRequest){
        var response = showTImeSeatService.unlockSeats(showtimeId, unlockSeatRequest);
        return ApiResponse.<List<ShowtimeSeatResponse>>builder()
                .success(true)
                .data(response)
                .message("Huỷ giữ chỗ thành công")
                .build();
    }
}
