package org.example.cinemaBooking.Service.Movie;

import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.cinemaBooking.DTO.Request.Movie.ReviewRequest;
import org.example.cinemaBooking.DTO.Response.Review.ReviewResponse;
import org.example.cinemaBooking.DTO.Response.Review.ReviewSummaryResponse;
import org.example.cinemaBooking.Entity.Movie;
import org.example.cinemaBooking.Entity.Review;
import org.example.cinemaBooking.Entity.UserEntity;
import org.example.cinemaBooking.Exception.AppException;
import org.example.cinemaBooking.Exception.ErrorCode;
import org.example.cinemaBooking.Mapper.ReviewMapper;
import org.example.cinemaBooking.Repository.MovieRepository;
import org.example.cinemaBooking.Repository.ReviewRepository;
import org.example.cinemaBooking.Repository.UserRepository;
import org.example.cinemaBooking.Shared.response.PageResponse;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReviewService {

    ReviewRepository reviewRepository;
    UserRepository userRepository;
    MovieRepository movieRepository;
    ReviewMapper reviewMapper;


    /**
     * Tạo mới một đánh giá cho bộ phim.
     * <p>Xoá bộ đệm các bài đánh giá chung và điểm trung bình của phim để cập nhật lại hệ thống.</p>
     *
     * @param request Dữ liệu đánh giá (chứa movieId, số sao, nội dung)
     * @return ReviewResponse thông tin đánh giá vừa tạo
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "movie-reviews", allEntries = true),
            @CacheEvict(value = "movie-rating", key = "#request.movieId()")
    })
    public ReviewResponse createReview(ReviewRequest request) {
        UserEntity user = getCurrentUser();

        Movie movie = movieRepository.findById(request.movieId())
                .orElseThrow(() -> new AppException(ErrorCode.MOVIE_NOT_FOUND));

        Optional<Review> existingOpt = reviewRepository.findByMovieAndUser(movie, user);

        if (existingOpt.isPresent()) {
            Review existing = existingOpt.get();

            if (existing.isDeleted()) {
                existing.restore();
                reviewMapper.updateFromRequest(request, existing);
                return reviewMapper.toResponse(reviewRepository.save(existing));
            }

            throw new AppException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        Review review = reviewMapper.toEntity(request);
        review.setUser(user);
        review.setMovie(movie);

        return reviewMapper.toResponse(reviewRepository.save(review));
    }


    /**
     * Cập nhật bài đánh giá của người dùng.
     * <p>Xoá bộ đệm danh sách đánh giá chung, điểm rating của tất cả phim và chi tiết của đánh giá này.</p>
     *
     * @param reviewId ID của bài đánh giá cần sửa
     * @param request  Dữ liệu đánh giá mới
     * @return ReviewResponse thông tin sau khi cập nhật
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "movie-reviews", allEntries = true),
            @CacheEvict(value = "movie-rating", allEntries = true),
            @CacheEvict(value = "review", key = "#reviewId")
    })
    public ReviewResponse updateReview(String reviewId, ReviewRequest request) {
        UserEntity user = getCurrentUser();

        Review review = reviewRepository.findById(reviewId)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        if (!Objects.equals(review.getUser().getId(), user.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        reviewMapper.updateFromRequest(request, review);

        try {
            review = reviewRepository.save(review);
        } catch (OptimisticLockingFailureException e) {
            throw new AppException(ErrorCode.CONCURRENT_UPDATE);
        }

        return reviewMapper.toResponse(review);
    }


    /**
     * Xoá ẩn (Soft Delete) một bài đánh giá.
     * <p>Xoá bộ đệm danh sách đánh giá chung, điểm rating chung và chi tiết bài đánh giá này.</p>
     *
     * @param reviewId ID của bài đánh giá cần xoá
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "movie-reviews", allEntries = true),
            @CacheEvict(value = "movie-rating", allEntries = true),
            @CacheEvict(value = "review", key = "#reviewId")
    })
    public void deleteReview(String reviewId) {
        UserEntity user = getCurrentUser();

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        if (!Objects.equals(review.getUser().getId(), user.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }


        if (review.isDeleted()) {
            return;
        }

        review.softDelete();
        reviewRepository.save(review);

        log.info("[REVIEW_SERVICE] User {} deleted review {}", user.getUsername(), reviewId);
    }


    /**
     * Lấy thông tin chi tiết một bài đánh giá (dựa vào ID).
     * <p>Dữ liệu được lưu vào cache "review" với key là ID của đánh giá.</p>
     *
     * @param reviewId ID của đánh giá cần tìm
     * @return ReviewResponse thông tin chi tiết đánh giá
     */
    @Cacheable(value = "review", key = "#reviewId")
    public ReviewResponse getReview(String reviewId) {
        Review review = reviewRepository.findWithMovieAndUserById(reviewId)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        return reviewMapper.toResponse(review);
    }


    /**
     * Lấy danh sách các bài đánh giá của một bộ phim (hỗ trợ phân trang và lọc theo số sao).
     * <p>Kết quả được lưu vào cache "movie-reviews" phân mảnh theo trang và số sao tối thiểu.</p>
     *
     * @param movieId       ID phim
     * @param page          Số trang hiện tại
     * @param size          Kích thước trang
     * @param minimumRating Số sao tối thiểu để lọc (nếu null sẽ chuyển thành 0 trong cache key)
     * @return PageResponse danh sách đánh giá tổng hợp
     */
    @Cacheable(value = "movie-reviews", key = "#movieId + '-' + #page + '-' + #size + '-' + (#minimumRating ?: 0)")
    public PageResponse<ReviewSummaryResponse> getReviewsByMovie(String movieId, int page, int size, Integer minimumRating) {

        int pageNumber = Math.max(page - 1, 0);

        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");

        if (minimumRating != null && minimumRating > 0) {
            sort = Sort.by(Sort.Direction.DESC, "rating").and(sort);
        }

        Pageable pageable = PageRequest.of(pageNumber, size, sort);

        Page<Review> reviews = (minimumRating != null && minimumRating > 0)
                ? reviewRepository.findByMovieIdAndRatingGreaterThanEqualAndDeletedFalse(movieId, minimumRating, pageable)
                : reviewRepository.findByMovieIdAndDeletedFalse(movieId, pageable);


        List<ReviewSummaryResponse> items = reviews.getContent()
                .stream()
                .map(reviewMapper::toSummary)
                .toList();

        return PageResponse.<ReviewSummaryResponse>builder()
                .page(page)
                .size(size)
                .totalElements(reviews.getTotalElements())
                .totalPages(reviews.getTotalPages())
                .items(items)
                .build();
    }


    /**
     * Tính điểm đánh giá trung bình của một bộ phim.
     * <p>Được lưu vào cache "movie-rating" để giảm tải tính toán liên tục từ database.</p>
     *
     * @param movieId ID bộ phim cần tính trung bình
     * @return Double số điểm trung bình (trả về 0.0 nếu chưa có ai đánh giá)
     */
    @Cacheable(value = "movie-rating", key = "#movieId")
    public Double getAverageRatingForMovie(String movieId) {
        Double avg = reviewRepository.getAverageRatingByMovieId(movieId);
        return avg != null ? avg : 0.0;
    }

    // ================= INTERNAL =================
    private UserEntity getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        String username = authentication.getName();

        return userRepository.findUserEntityByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }
}