package org.example.cinemaBooking.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.cinemaBooking.DTO.Request.Cinema.CreateCinemaRequest;
import org.example.cinemaBooking.DTO.Request.Cinema.UpdateCinemaRequest;
import org.example.cinemaBooking.DTO.Response.Cinema.CinemaMovieResponse;
import org.example.cinemaBooking.DTO.Response.Cinema.CinemaResponse;
import org.example.cinemaBooking.DTO.Response.Room.RoomBasicResponse;
import org.example.cinemaBooking.Service.Cinema.CinemaService;
import org.example.cinemaBooking.Shared.constant.ApiPaths;
import org.example.cinemaBooking.Shared.response.ApiResponse;
import org.example.cinemaBooking.Shared.response.PageResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Cinema", description = "quản lý rạp chiếu")
@RequestMapping(ApiPaths.API_V1 + ApiPaths.Cinema.BASE)
public class CinemaController {

    CinemaService cinemaService;

    @Operation(summary = "Tạo rạp chiếu mới",
            description = "Tạo một rạp chiếu mới. Yêu cầu quyền ADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ApiResponse<CinemaResponse> createCinema(@RequestBody @Valid CreateCinemaRequest request){
        CinemaResponse response = cinemaService.createCinema(request);
        return ApiResponse.<CinemaResponse>builder()
                .success(true)
                .message("Cinema created successfully")
                .data(response)
                .build();
    }

    @Operation(summary = "Cập nhật rạp chiếu",
            description = "Cập nhật thông tin rạp chiếu theo ID. Yêu cầu quyền ADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ApiResponse<CinemaResponse> updateCinema(
            @PathVariable String id,
            @RequestBody @Valid UpdateCinemaRequest request
    ){
        return ApiResponse.<CinemaResponse>builder()
                .success(true)
                .message("Cinema updated successfully")
                .data(cinemaService.updateCinema(id, request))
                .build();
    }

    @Operation(summary = "Xóa rạp chiếu",
            description = "Xóa rạp chiếu theo ID. Yêu cầu quyền ADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteCinema(@PathVariable String id){
        cinemaService.deleteCinemaById(id);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Cinema deleted successfully")
                .build();
    }

    @Operation(summary = "Lấy chi tiết rạp chiếu",
            description = "Lấy chi tiết rạp chiếu theo ID.", security = {})
    @GetMapping("/{id}")
    public ApiResponse<CinemaResponse> getCinema(@PathVariable String id){
        return ApiResponse.<CinemaResponse>builder()
                .success(true)
                .message("Cinema retrieved successfully")
                .data(cinemaService.getCinemaById(id))
                .build();
    }

    @Operation(summary = "Chuyển trạng thái hoạt động của rạp chiếu",
            description = "Chuyển trạng thái hoạt động của rạp chiếu (active/inactive) theo ID. Yêu cầu quyền ADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/toggle-status")
    public ApiResponse<CinemaResponse> toggleStatus(@PathVariable String id){
        cinemaService.toggleCinemaStatus(id);
        return ApiResponse.<CinemaResponse>builder()
                .success(true)
                .message("Cinema status toggled successfully")
                .data(cinemaService.getCinemaById(id))
                .build();
    }

    @Operation(summary = "Lấy danh sách rạp chiếu",
            description = "Lấy danh sách phân trang các rạp chiếu. Hỗ trợ tìm kiếm theo tên.",
            security = {})
    @GetMapping
    public ApiResponse<PageResponse<CinemaResponse>> getAllCinemas(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String keyword
    ){
        return ApiResponse.<PageResponse<CinemaResponse>>builder()
                .success(true)
                .message("Cinemas retrieved successfully")
                .data(cinemaService.getAllCinemas(page, size, sortBy, direction, keyword))
                .build();
    }

    @Operation(summary = "Lấy danh sách phim theo rạp chiếu và ngày chiếu",
            description = "Lấy danh sách phân trang các phim được chiếu tại một rạp chiếu cụ thể vào một ngày cụ thể.",
            security = {})
    @GetMapping("/{cinemaId}/movies")
    public ApiResponse<PageResponse<CinemaMovieResponse>> getMoviesByCinemaAndDate(
            @PathVariable String cinemaId,
            @RequestParam LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "title") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        log.info("[CINEMA_CONTROLLER] Getting movies for cinema: {}, date: {}, page: {}",
                cinemaId, date, page);

        var movies = cinemaService.getMoviesByCinemaAndDate(
                cinemaId, date, page, size, sortBy, direction);

        return ApiResponse.<PageResponse<CinemaMovieResponse>>builder()
                .success(true)
                .message("Movies retrieved successfully")
                .data(movies)
                .build();
    }

    @Operation(summary = "Lấy danh sách phòng chiếu theo rạp chiếu",
            description = "Lấy danh sách phân trang các phòng chiếu thuộc một rạp chiếu cụ thể.",
            security = {})
    @GetMapping("/{cinemaId}/rooms")
    public ApiResponse<PageResponse<RoomBasicResponse>> getRoomsByCinemaId(
            @PathVariable String cinemaId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        log.info("[ROOM_CONTROLLER] Getting rooms for cinema id: {} - page: {}, size: {}",
                cinemaId, page, size);

        PageResponse<RoomBasicResponse> response = cinemaService.getRoomsByCinema(
                cinemaId, page, size, sortBy, direction);

        return ApiResponse.<PageResponse<RoomBasicResponse>>builder()
                .success(true)
                .message("Rooms retrieved successfully")
                .data(response)
                .build();
    }

}