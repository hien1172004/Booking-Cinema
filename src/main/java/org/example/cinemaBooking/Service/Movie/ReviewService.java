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


    @Transactional
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


    @Transactional
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


    @Transactional
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


    public ReviewResponse getReview(String reviewId) {
        Review review = reviewRepository.findWithMovieAndUserById(reviewId)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        return reviewMapper.toResponse(review);
    }


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