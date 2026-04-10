package org.example.cinemaBooking.Controller.Movie;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.cinemaBooking.DTO.Request.Movie.ReviewRequest;
import org.example.cinemaBooking.DTO.Response.Review.ReviewResponse;
import org.example.cinemaBooking.DTO.Response.Review.ReviewSummaryResponse;
import org.example.cinemaBooking.Service.Movie.ReviewService;
import org.example.cinemaBooking.Shared.constant.ApiPaths;
import org.example.cinemaBooking.Shared.response.ApiResponse;
import org.example.cinemaBooking.Shared.response.PageResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping(ApiPaths.API_V1 + ApiPaths.Review.BASE)
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Review", description = "quản lý đánh giá phim")
public class ReviewController {
    ReviewService reviewService;

    @Operation(summary = "Tạo đánh giá mới",
            description = "Tạo một đánh giá mới cho một phim. Yêu cầu người dùng đã đăng nhập.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    ApiResponse<ReviewResponse> createReview(@RequestBody @Valid ReviewRequest reviewRequest) {
        ReviewResponse reviewResponse = reviewService.createReview(reviewRequest);
        log.info("[ReviewController] createReview - reviewId: {}", reviewResponse.id());
        return ApiResponse.<ReviewResponse>builder()
                .success(true)
                .data(reviewResponse)
                .build();
    }

    @Operation(summary = "Cập nhật đánh giá",
            description = "Cập nhật một đánh giá đã tồn tại. Yêu cầu người dùng đã đăng nhập và là chủ sở hữu của đánh giá.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{reviewId}")
    ApiResponse<ReviewResponse> updateReview(@PathVariable String reviewId, @RequestBody @Valid ReviewRequest reviewRequest) {
        ReviewResponse reviewResponse = reviewService.updateReview(reviewId, reviewRequest);
        log.info("[ReviewController] updateReview - reviewId: {}", reviewResponse.id());
        return ApiResponse.<ReviewResponse>builder()
                .success(true)
                .data(reviewResponse)
                .build();
    }

    @Operation(summary = "Xóa đánh giá",
            description = "Xóa một đánh giá đã tồn tại. Yêu cầu người dùng đã đăng nhập và là chủ sở hữu của đánh giá.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{reviewId}")
    ApiResponse<Void> deleteReview(@PathVariable String reviewId) {
        reviewService.deleteReview(reviewId);
        log.info("[ReviewController] deleteReview - reviewId: {}", reviewId);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }

    @Operation(summary = "Lấy thông tin đánh giá",
            description = "Lấy thông tin chi tiết của một đánh giá theo ID.")
    @GetMapping("/{reviewId}")
    ApiResponse<ReviewResponse> getReview(@PathVariable String reviewId) {
        ReviewResponse reviewResponse = reviewService.getReview(reviewId);
        log.info("[ReviewController] getReview - reviewId: {}", reviewResponse.id());
        return ApiResponse.<ReviewResponse>builder()
                .success(true)
                .data(reviewResponse)
                .build();
    }

    @Operation(summary = "Lấy danh sách đánh giá theo phim",
            description = "Lấy danh sách đánh giá cho một phim theo ID của phim. Hỗ trợ phân trang và lọc theo đánh giá tối thiểu.")
    @GetMapping(ApiPaths.Movie.BASE + "/{movieId}")
    ApiResponse<PageResponse<ReviewSummaryResponse>> getReviewByMovie(
            @PathVariable String movieId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer minimumRating
    ){
        var response = reviewService.getReviewsByMovie(movieId, page, size, minimumRating);
        log.info("[ReviewController] getReviewByMovie - movieId: {}, page: {}, size: {}", movieId, page, size);
        return ApiResponse.<PageResponse<ReviewSummaryResponse>>builder()
                .success(true)
                .data(response)
                .build();
    }

    @Operation(summary = "Lấy điểm đánh giá trung bình của phim",
            description = "Lấy điểm đánh giá trung bình của một phim theo ID của phim.")
    @GetMapping(ApiPaths.Movie.BASE + "/{movieId}/average-rating")
    ApiResponse<Double> getAverageRatingByMovie(@PathVariable String movieId) {
        Double averageRating = reviewService.getAverageRatingForMovie(movieId);
        log.info("[ReviewController] getAverageRatingByMovie - movieId: {}, averageRating: {}", movieId, averageRating);
        return ApiResponse.<Double>builder()
                .success(true)
                .data(averageRating)
                .build();
    }
}
