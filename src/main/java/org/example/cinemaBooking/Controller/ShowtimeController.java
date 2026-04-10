package org.example.cinemaBooking.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.cinemaBooking.DTO.Request.Showtime.CreateShowtimeRequest;
import org.example.cinemaBooking.DTO.Request.Showtime.ShowtimeFilterRequest;
import org.example.cinemaBooking.DTO.Request.Showtime.UpdateShowtimeRequest;
import org.example.cinemaBooking.DTO.Response.Showtime.ShowtimeDetailResponse;
import org.example.cinemaBooking.DTO.Response.Showtime.ShowtimeSummaryResponse;
import org.example.cinemaBooking.Service.Showtime.ShowtimeService;
import org.example.cinemaBooking.Shared.constant.ApiPaths;
import org.example.cinemaBooking.Shared.enums.Language;
import org.example.cinemaBooking.Shared.enums.ShowTimeStatus;
import org.example.cinemaBooking.Shared.response.ApiResponse;
import org.example.cinemaBooking.Shared.response.PageResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping(ApiPaths.API_V1 + ApiPaths.Showtime.BASE)
@Tag(name = "Showtime", description = "quản lý suất chiếu")
public class ShowtimeController {
    ShowtimeService showtimeService;

    @Operation(summary = "Tạo suất chiếu",
            description = "Tạo suất chiếu mới (chỉ ADMIN).",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ShowtimeDetailResponse> createShowtime(@RequestBody @Valid CreateShowtimeRequest request) {
        ShowtimeDetailResponse response = showtimeService.createShowtime(request);
        log.info("[SHOWTIME_CONTROLLER] Created showtime with id: {}", response.id());
        return ApiResponse.<ShowtimeDetailResponse>builder().
                success(true).message("Tạo suất chiếu thành công")
                .data(response).
                build();
    }

    @Operation(summary = "Cập nhật suất chiếu",
            description = "Cập nhật thông tin suất chiếu theo ID (chỉ ADMIN).",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ShowtimeDetailResponse> updateShowtime(@PathVariable String id, @RequestBody @Valid UpdateShowtimeRequest request) {
        var response = showtimeService.updateShowtime(id, request);
        log.info("[SHOWTIME_CONTROLLER] Updated showtime with id: {}", response.id());
        return ApiResponse.<ShowtimeDetailResponse>builder()
                .success(true)
                .message("Cập nhật suất chiếu thành công")
                .data(response).
                build();
    }

    @Operation(summary = "Hủy suất chiếu",
            description = "Hủy một suất chiếu theo ID (chỉ ADMIN).",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ShowtimeDetailResponse> cancelShowtime(@PathVariable String id) {
        var response = showtimeService.cancelShowtime(id);
        log.info("[SHOWTIME_CONTROLLER] Cancelled showtime with id: {}", response.id());
        return ApiResponse.<ShowtimeDetailResponse>builder()
                .success(true)
                .message("Hủy suất chiếu thành công")
                .data(response)
                .build();
    }

    @Operation(summary = "Xóa suất chiếu",
            description = "Xóa một suất chiếu theo ID (chỉ ADMIN).",
            security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteShowtime(@PathVariable String id) {
        showtimeService.deleteShowtime(id);
        log.info("[SHOWTIME_CONTROLLER] Deleted showtime with id: {}", id);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Xóa suất chiếu thành công")
                .build();
    }


    @Operation(summary = "Lấy chi tiết suất chiếu",
            description = "Lấy chi tiết suất chiếu theo ID.")
    @GetMapping("/{id}")
    public ApiResponse<ShowtimeDetailResponse> getShowtimeById(@PathVariable String id) {
        var response = showtimeService.getShowtimeById(id);
        log.info("[SHOWTIME_CONTROLLER] Retrieved showtime with id: {}", id);
        return ApiResponse.<ShowtimeDetailResponse>builder()
                .success(true)
                .message("Lấy chi tiết suất chiếu thành công")
                .data(response)
                .build();
    }

    @Operation(summary = "Lấy danh sách suất chiếu",
            description = "Lấy danh sách suất chiếu, hỗ trợ lọc và phân trang. Yêu cầu quyền STAFF/ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @GetMapping
    public ApiResponse<PageResponse<ShowtimeSummaryResponse>> getShowtimes(
            @RequestParam(required = false) String movieId,
            @RequestParam(required = false) String cinemaId,
            @RequestParam(required = false) String roomId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Language language,
            @RequestParam(required = false) ShowTimeStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        ShowtimeFilterRequest request = new ShowtimeFilterRequest(
                movieId,
                cinemaId,
                roomId,
                date,
                language,
                status,
                keyword,
                page,
                size
        );

        PageResponse<ShowtimeSummaryResponse> response = showtimeService.getShowtime(request);

        log.info("Retrieved showtimes - movieId: {}, cinemaId: {}, roomId: {}, date: {}, " +
                        "language: {}, status: {}, keyword: {}, page: {}, size: {}",
                movieId, cinemaId, roomId, date, language, status, keyword, page, size);

        return ApiResponse.<PageResponse<ShowtimeSummaryResponse>>builder()
                .success(true)
                .message("Lấy danh sách suất chiếu thành công")
                .data(response)
                .build();
    }

    /**
     * GET /api/v1/showtimes/by-movie/{movieId}?date=2025-08-01
     * Dùng cho màn hình chọn suất chiếu theo phim.
     */
    @Operation(summary = "Lấy suất chiếu theo phim và ngày",
            description = "Lấy danh sách suất chiếu cho một phim vào một ngày cụ thể.")
    @GetMapping("/by-movie/{movieId}")
    public ApiResponse<List<ShowtimeSummaryResponse>> getShowtimesByMovieAndDate(
            @PathVariable String movieId,
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now()}") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date){
        var response = showtimeService.getShowtimeByMovieAndDate(movieId, date);
        log.info("[SHOWTIME_CONTROLLER] Retrieved showtimes for movieId={} on date={}", movieId, date);
        return ApiResponse.<List<ShowtimeSummaryResponse>>builder()
                .success(true)
                .message("Lấy suất chiếu thành công")
                .data(response)
                .build();
    }

    /**
     * GET /api/v1/showtimes/by-cinema/{cinemaId}?date=2025-08-01
     * Dùng cho màn hình lịch chiếu theo rạp.
     */
    @Operation(summary = "Lấy suất chiếu theo rạp và ngày",
            description = "Lấy danh sách suất chiếu trong một rạp cụ thể theo ngày.")
    @GetMapping("/by-cinema/{cinemaId}")
    public ApiResponse<List<ShowtimeSummaryResponse>> getShowtimesByCinemaAndDate(
            @PathVariable String cinemaId,
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now()}") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        var response = showtimeService.getShowtimesByCinemaAndDate(cinemaId, date);
        log.info("[SHOWTIME_CONTROLLER] Retrieved showtimes for cinemaId={} on date={}", cinemaId, date);
        return ApiResponse.<List<ShowtimeSummaryResponse>>builder()
                .success(true)
                .message("Lấy suất chiếu thành công")
                .data(response)
                .build();
    }
}
