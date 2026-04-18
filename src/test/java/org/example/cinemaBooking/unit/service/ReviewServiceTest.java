package org.example.cinemaBooking.unit.service;

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
import org.example.cinemaBooking.Service.Movie.ReviewService;
import org.example.cinemaBooking.Shared.response.PageResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MovieRepository movieRepository;
    @Mock
    private ReviewMapper reviewMapper;

    @Mock
    private Authentication authentication;
    @Mock
    private SecurityContext securityContext;

    private MockedStatic<SecurityContextHolder> mockedSecurityContextHolder;

    @InjectMocks
    private ReviewService reviewService;

    private UserEntity user;
    private Movie movie;
    private Review review;
    private ReviewRequest reviewRequest;

    @BeforeEach
    void setUp() {
        user = new UserEntity();
        user.setId("u-001");
        user.setUsername("testuser");

        movie = new Movie();
        movie.setId("m-001");

        review = new Review();
        review.setId("r-001");
        review.setUser(user);
        review.setMovie(movie);
        review.setRating(5);
        review.setDeleted(false);

        reviewRequest = new ReviewRequest("m-001", 5, "Great movie!");
    }

    private void mockAuthentication() {
        mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class);
        mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");
        when(userRepository.findUserEntityByUsername("testuser")).thenReturn(Optional.of(user));
    }

    private void mockAuthenticationUnauthenticated() {
        mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class);
        mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);
    }

    @AfterEach
    void tearDown() {
        if (mockedSecurityContextHolder != null) {
            mockedSecurityContextHolder.close();
        }
    }

    @Nested
    class CreateReviewTests {
        @BeforeEach
        void init() {
            mockAuthentication();
        }

        @Test
        void createReview_Success() {
            when(movieRepository.findById("m-001")).thenReturn(Optional.of(movie));
            when(reviewRepository.findByMovieAndUser(movie, user)).thenReturn(Optional.empty());
            when(reviewMapper.toEntity(any())).thenReturn(review);
            when(reviewRepository.save(any())).thenReturn(review);
            when(reviewMapper.toResponse(any())).thenReturn(mock(ReviewResponse.class));

            ReviewResponse response = reviewService.createReview(reviewRequest);

            assertThat(response).isNotNull();
            verify(reviewRepository).save(any());
        }

        @Test
        void createReview_MovieNotFound_ThrowsException() {
            when(movieRepository.findById("m-001")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.createReview(reviewRequest))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MOVIE_NOT_FOUND);
        }

        @Test
        void createReview_ReviewAlreadyExistsAndNotDeleted_ThrowsException() {
            when(movieRepository.findById("m-001")).thenReturn(Optional.of(movie));
            when(reviewRepository.findByMovieAndUser(movie, user)).thenReturn(Optional.of(review));

            assertThatThrownBy(() -> reviewService.createReview(reviewRequest))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        @Test
        void createReview_ReviewExistsAndDeleted_RestoresReview() {
            review.setDeleted(true);
            when(movieRepository.findById("m-001")).thenReturn(Optional.of(movie));
            when(reviewRepository.findByMovieAndUser(movie, user)).thenReturn(Optional.of(review));
            when(reviewRepository.save(any())).thenReturn(review);
            when(reviewMapper.toResponse(any())).thenReturn(mock(ReviewResponse.class));

            ReviewResponse response = reviewService.createReview(reviewRequest);

            assertThat(response).isNotNull();
            assertThat(review.isDeleted()).isFalse();
            verify(reviewMapper).updateFromRequest(reviewRequest, review);
        }
    }

    @Nested
    class UpdateReviewTests {
        @BeforeEach
        void init() {
            mockAuthentication();
        }

        @Test
        void updateReview_Success() {
            when(reviewRepository.findById("r-001")).thenReturn(Optional.of(review));
            when(reviewRepository.save(any())).thenReturn(review);
            when(reviewMapper.toResponse(any())).thenReturn(mock(ReviewResponse.class));

            ReviewResponse response = reviewService.updateReview("r-001", reviewRequest);

            assertThat(response).isNotNull();
            verify(reviewRepository).save(any());
            verify(reviewMapper).updateFromRequest(reviewRequest, review);
        }

        @Test
        void updateReview_NotFound_ThrowsException() {
            when(reviewRepository.findById("invalid")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.updateReview("invalid", reviewRequest))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REVIEW_NOT_FOUND);
        }

        @Test
        void updateReview_Unauthorized_ThrowsException() {
            UserEntity otherUser = new UserEntity();
            otherUser.setId("u-other");
            review.setUser(otherUser);

            when(reviewRepository.findById("r-001")).thenReturn(Optional.of(review));

            assertThatThrownBy(() -> reviewService.updateReview("r-001", reviewRequest))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);
        }

        @Test
        void updateReview_ConcurrentUpdate_ThrowsException() {
            when(reviewRepository.findById("r-001")).thenReturn(Optional.of(review));
            when(reviewRepository.save(any())).thenThrow(new OptimisticLockingFailureException("concurrent"));

            assertThatThrownBy(() -> reviewService.updateReview("r-001", reviewRequest))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CONCURRENT_UPDATE);
        }
    }

    @Nested
    class DeleteReviewTests {
        @BeforeEach
        void init() {
            mockAuthentication();
        }

        @Test
        void deleteReview_Success() {
            when(reviewRepository.findById("r-001")).thenReturn(Optional.of(review));

            reviewService.deleteReview("r-001");

            assertThat(review.isDeleted()).isTrue();
            verify(reviewRepository).save(review);
        }

        @Test
        void deleteReview_AlreadyDeleted_DoesNothing() {
            review.setDeleted(true);
            when(reviewRepository.findById("r-001")).thenReturn(Optional.of(review));

            reviewService.deleteReview("r-001");

            verify(reviewRepository, never()).save(any());
        }

        @Test
        void deleteReview_NotFound_ThrowsException() {
            when(reviewRepository.findById("invalid")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.deleteReview("invalid"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REVIEW_NOT_FOUND);
        }

        @Test
        void deleteReview_Unauthorized_ThrowsException() {
            UserEntity otherUser = new UserEntity();
            otherUser.setId("u-other");
            review.setUser(otherUser);

            when(reviewRepository.findById("r-001")).thenReturn(Optional.of(review));

            assertThatThrownBy(() -> reviewService.deleteReview("r-001"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);
        }
    }

    @Nested
    class GetReviewTests {
        @Test
        void getReview_Success() {
            when(reviewRepository.findWithMovieAndUserById("r-001")).thenReturn(Optional.of(review));
            when(reviewMapper.toResponse(any())).thenReturn(mock(ReviewResponse.class));

            ReviewResponse response = reviewService.getReview("r-001");

            assertThat(response).isNotNull();
        }

        @Test
        void getReview_NotFound_ThrowsException() {
            when(reviewRepository.findWithMovieAndUserById("invalid")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.getReview("invalid"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REVIEW_NOT_FOUND);
        }

        @Test
        void getReviewsByMovie_AllRatings_Success() {
            Page<Review> reviewPage = new PageImpl<>(Collections.singletonList(review));
            when(reviewRepository.findByMovieIdAndDeletedFalse(eq("m-001"), any(Pageable.class))).thenReturn(reviewPage);
            when(reviewMapper.toSummary(any())).thenReturn(mock(ReviewSummaryResponse.class));

            PageResponse<ReviewSummaryResponse> response = reviewService.getReviewsByMovie("m-001", 1, 10, null);

            assertThat(response.getItems()).hasSize(1);
            verify(reviewRepository).findByMovieIdAndDeletedFalse(anyString(), any(Pageable.class));
        }

        @Test
        void getReviewsByMovie_WithMinimumRating_Success() {
            Page<Review> reviewPage = new PageImpl<>(Collections.singletonList(review));
            when(reviewRepository.findByMovieIdAndRatingGreaterThanEqualAndDeletedFalse(eq("m-001"), eq(4), any(Pageable.class))).thenReturn(reviewPage);
            when(reviewMapper.toSummary(any())).thenReturn(mock(ReviewSummaryResponse.class));

            PageResponse<ReviewSummaryResponse> response = reviewService.getReviewsByMovie("m-001", 1, 10, 4);

            assertThat(response.getItems()).hasSize(1);
            verify(reviewRepository).findByMovieIdAndRatingGreaterThanEqualAndDeletedFalse(anyString(), anyInt(), any(Pageable.class));
        }

        @Test
        void getAverageRatingForMovie_ReturnsAverage() {
            when(reviewRepository.getAverageRatingByMovieId("m-001")).thenReturn(4.5);
            Double avg = reviewService.getAverageRatingForMovie("m-001");
            assertThat(avg).isEqualTo(4.5);
        }

        @Test
        void getAverageRatingForMovie_ReturnsZeroWhenNull() {
            when(reviewRepository.getAverageRatingByMovieId("m-001")).thenReturn(null);
            Double avg = reviewService.getAverageRatingForMovie("m-001");
            assertThat(avg).isEqualTo(0.0);
        }
    }

    @Nested
    class AuthTests {
        @Test
        void getCurrentUser_Unauthenticated_ThrowsException() {
            mockAuthenticationUnauthenticated();

            assertThatThrownBy(() -> reviewService.createReview(reviewRequest))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);
        }

        @Test
        void getCurrentUser_UserNotFound_ThrowsException() {
            mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class);
            mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getName()).thenReturn("unknown");
            when(userRepository.findUserEntityByUsername("unknown")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.createReview(reviewRequest))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
        }
    }
}
