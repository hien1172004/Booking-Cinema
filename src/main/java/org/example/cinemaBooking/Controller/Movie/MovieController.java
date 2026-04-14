package org.example.cinemaBooking.Controller.Movie;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.cinemaBooking.DTO.Request.Movie.CreateMovieRequest;
import org.example.cinemaBooking.DTO.Request.Movie.UpdateMovieRequest;
import org.example.cinemaBooking.DTO.Request.Movie.UpdateMovieStatusRequest;
import org.example.cinemaBooking.DTO.Response.Movie.MovieRecommendResponse;
import org.example.cinemaBooking.DTO.Response.Movie.MovieResponse;
import org.example.cinemaBooking.Service.Movie.MovieService;
import org.example.cinemaBooking.Service.Movie.PeopleService;
import org.example.cinemaBooking.Shared.constant.ApiPaths;
import org.example.cinemaBooking.Shared.constraints.RateLimit;
import org.example.cinemaBooking.Shared.response.ApiResponse;
import org.example.cinemaBooking.Shared.response.PageResponse;
import org.example.cinemaBooking.Shared.enums.AgeRating;
import org.example.cinemaBooking.Shared.enums.MovieStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping(ApiPaths.API_V1 + ApiPaths.Movie.BASE)
@Tag(name = "Movie", description = "quản lý phim")
public class MovieController {
    MovieService movieService;
    PeopleService peopleService;

    @Operation(summary = "Tạo phim mới",
            description = "Tạo một phim mới. Yêu cầu quyền ADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<MovieResponse> createMovie(@RequestBody @Valid CreateMovieRequest request) {
        MovieResponse movieResponse = movieService.creatMovie(request);
        log.info("[MOVIE CONTROLLER] Movie {} has been created", movieResponse.getId());
        return ApiResponse.<MovieResponse>builder()
                .success(true)
                .data(movieResponse)
                .build();
    }

    @Operation(summary = "Cập nhật thông tin phim",
            description = "Cập nhật thông tin phim theo ID. Yêu cầu quyền ADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<MovieResponse> updateMovie(@PathVariable String id, @RequestBody @Valid UpdateMovieRequest request) {
        MovieResponse movieResponse = movieService.updateMovie(id, request);
        log.info("[MOVIE CONTROLLER] Movie {} has been updated", movieResponse.getId());
        return ApiResponse.<MovieResponse>builder()
                .success(true)
                .data(movieResponse)
                .build();
    }

    @Operation(summary = "Xóa phim",
            description = "Xóa phim theo ID. Yêu cầu quyền ADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteMovie(@PathVariable String id) {
        movieService.deleteMovie(id);
        log.info("[MOVIE CONTROLLER] Movie {} has been deleted", id);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Movie deleted successfully")
                .build();
    }

    @Operation(summary = "Cập nhật trạng thái phim",
            description = "Cập nhật trạng thái phim theo ID. Yêu cầu quyền ADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<MovieResponse> updateMovieStatus(@PathVariable String id, @RequestBody @Valid UpdateMovieStatusRequest request) {
        MovieResponse movieResponse = movieService.updateMovieStatus(id, request);
        log.info("[MOVIE CONTROLLER] Movie {} status has been updated to {}", movieResponse.getId(), movieResponse.getStatus());
        return ApiResponse.<MovieResponse>builder()
                .success(true)
                .data(movieResponse)
                .build();
    }

    @Operation(summary = "Lấy danh sách phim",
            description = "Lấy danh sách phim với các tùy chọn lọc và phân trang. Yêu cầu quyền ADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @RateLimit(capacity = 30, refillPerMinute = 30)
    public ApiResponse<PageResponse<MovieResponse>> getMovies(

            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) MovieStatus status,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) AgeRating ageRating,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "releaseDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {

        PageResponse<MovieResponse> movies = movieService.getMovies(
                keyword,
                status,
                categoryId,
                ageRating,
                page,
                size,
                sortBy,
                sortDir
        );
        log.info("[MOVIE CONTROLLER] Get movies with keyword: {}, status: {}, categoryId: {}, ageRating: {}, page: {}, size: {}, sortBy: {}, sortDir: {}",
                keyword, status, categoryId, ageRating, page, size, sortBy, sortDir);
        return ApiResponse.<PageResponse<MovieResponse>>builder()
                .success(true)
                .data(movies)
                .build();
    }

    @Operation(summary = "Lấy chi tiết phim theo ID",
            description = "Lấy chi tiết phim theo ID.")
    @GetMapping("/{id}")
    @RateLimit(capacity = 60, refillPerMinute = 60)
    public ApiResponse<MovieResponse> getMovieDetailById(@PathVariable String id) {
        MovieResponse movieResponse = movieService.getMovieById(id);
        log.info("[MOVIE CONTROLLER] Get movie detail for movie with id: {}", id);
        return ApiResponse.<MovieResponse>builder()
                .success(true)
                .data(movieResponse)
                .build();
    }

    @Operation(summary = "Lấy chi tiết phim theo slug",
            description = "Lấy chi tiết phim theo slug. Không yêu cầu quyền.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/slug/{slug}")
    @RateLimit(capacity = 60, refillPerMinute = 60)
    public ApiResponse<MovieResponse> getMovieDetailBySlug(@PathVariable String slug) {
        MovieResponse movieResponse = movieService.getMovieBySlug(slug);
        log.info("[MOVIE CONTROLLER] Get movie detail for movie with slug: {}", slug);
        return ApiResponse.<MovieResponse>builder()
                .success(true)
                .data(movieResponse)
                .build();
    }

    @Operation(summary = "Lấy danh sách phim đang chiếu",
            description = "Lấy danh sách phim đang chiếu với phân trang. Không yêu cầu quyền.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping(ApiPaths.Movie.NOW_SHOWING)
    @RateLimit(capacity = 60, refillPerMinute = 60)
    public ApiResponse<PageResponse<MovieResponse>> getNowShowingMovies(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size){
        PageResponse<MovieResponse> movies = movieService.getMoviesIsNowShowing(page, size);
        return ApiResponse.<PageResponse<MovieResponse>>builder()
                .success(true)
                .data(movies)
                .build();
    }

    @Operation(summary = "Lấy danh sách phim sắp chiếu",
            description = "Lấy danh sách phim sắp chiếu với phân trang. Không yêu cầu quyền.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping(ApiPaths.Movie.COMING_SOON)
    @RateLimit(capacity = 60, refillPerMinute = 60)
    public ApiResponse<PageResponse<MovieResponse>> getComingSoonMovies(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<MovieResponse> movies = movieService.getMoviesIsComingSoon(page, size);
        return ApiResponse.<PageResponse<MovieResponse>>builder()
                .success(true)
                .data(movies)
                .build();
    }


    @Operation(summary = "Tìm kiếm phim",
            description = "Tìm kiếm phim theo từ khóa với phân trang. Không yêu cầu quyền.")
    @GetMapping(ApiPaths.Movie.SEARCH + "/{keyword}")
    @RateLimit(capacity = 30, refillPerMinute = 30)
    public ApiResponse<PageResponse<MovieResponse>> searchMovies(
            @PathVariable String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<MovieResponse> movies = movieService.searchMovie(page, size, keyword);
        return ApiResponse.<PageResponse<MovieResponse>>builder()
                .success(true)
                .data(movies)
                .build();
    }

    @Operation(summary = "Gợi ý phim",
            description = "Lấy danh sách các bộ phim được đề xuất dựa trên độ hot (doanh thu, số lượng vé, đánh giá). Không yêu cầu quyền.")
    @GetMapping("/recommend")
    @RateLimit(capacity = 60, refillPerMinute = 60)
    public ApiResponse<java.util.List<MovieRecommendResponse>> recommendMovies() {
        List<MovieRecommendResponse> movies = movieService.recommend();
        return ApiResponse.<java.util.List<MovieRecommendResponse>>builder()
                .success(true)
                .message("[MOVIE CONTROLLER] Recommend movies based on hotness")
                .data(movies)
                .build();
    }

}
