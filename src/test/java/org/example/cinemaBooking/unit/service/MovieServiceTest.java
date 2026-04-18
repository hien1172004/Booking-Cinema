package org.example.cinemaBooking.unit.service;

import org.example.cinemaBooking.DTO.Request.Movie.CreateMovieRequest;
import org.example.cinemaBooking.DTO.Request.Movie.UpdateMovieRequest;
import org.example.cinemaBooking.DTO.Response.Movie.MovieRecommendResponse;
import org.example.cinemaBooking.DTO.Response.Movie.MovieResponse;
import org.example.cinemaBooking.DTO.Response.Movie.MovieStats;
import org.example.cinemaBooking.Entity.Category;
import org.example.cinemaBooking.Entity.Movie;
import org.example.cinemaBooking.Exception.AppException;
import org.example.cinemaBooking.Exception.ErrorCode;
import org.example.cinemaBooking.Mapper.MovieMapper;
import org.example.cinemaBooking.Repository.CategoryRepository;
import org.example.cinemaBooking.Repository.MovieRepository;
import org.example.cinemaBooking.Service.Movie.MovieService;
import org.example.cinemaBooking.Shared.enums.AgeRating;
import org.example.cinemaBooking.Shared.enums.MovieStatus;
import org.example.cinemaBooking.Shared.response.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MovieServiceTest {

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private MovieMapper movieMapper;

    @InjectMocks
    private MovieService movieService;

    private Movie movie;
    private MovieResponse movieResponse;
    private CreateMovieRequest createRequest;
    private Category category;

    @BeforeEach
    void setUp() {
        category = Category.builder()
                .name("Action")
                .build();
        category.setId("cat-001");

        movie = Movie.builder()
                .title("Dune: Part Two")
                .slug("dune-part-two")
                .duration(166)
                .releaseDate(LocalDate.of(2024, 3, 1))
                .ageRating(AgeRating.C13)
                .status(MovieStatus.NOW_SHOWING)
                .categories(new HashSet<>(Collections.singletonList(category)))
                .build();
        movie.setId("movie-001");

        movieResponse = MovieResponse.builder()
                .id("movie-001")
                .title("Dune: Part Two")
                .slug("dune-part-two")
                .build();

        createRequest = CreateMovieRequest.builder()
                .title("Dune: Part Two")
                .slug("dune-part-two")
                .duration(166)
                .releaseDate(LocalDate.of(2024, 3, 1))
                .ageRating("C13")
                .categoryIds(Collections.singleton("cat-001"))
                .build();
    }

    @Nested
    class CreateMovieTests {
        @Test
        void creatMovie_Success() {
            // Given
            when(movieRepository.findBySlug(createRequest.getSlug())).thenReturn(Optional.empty());
            when(movieMapper.toMovie(createRequest)).thenReturn(movie);
            when(categoryRepository.findAllById(createRequest.getCategoryIds()))
                    .thenReturn(Collections.singletonList(category));
            when(movieRepository.save(any(Movie.class))).thenReturn(movie);
            when(movieMapper.toMovieResponse(any(Movie.class))).thenReturn(movieResponse);

            // When
            MovieResponse result = movieService.creatMovie(createRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo(movieResponse.getTitle());
            verify(movieRepository).save(any(Movie.class));
        }

        @Test
        void creatMovie_SlugExists_ThrowsException() {
            // Given
            when(movieRepository.findBySlug(createRequest.getSlug())).thenReturn(Optional.of(movie));

            // When & Then
            assertThatThrownBy(() -> movieService.creatMovie(createRequest))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MOVIE_SLUG_ALREADY_EXISTS);
        }

        @Test
        void creatMovie_CategoryNotFound_ThrowsException() {
            // Given
            when(movieRepository.findBySlug(createRequest.getSlug())).thenReturn(Optional.empty());
            when(movieMapper.toMovie(createRequest)).thenReturn(movie);
            // find 1 but expect more
            createRequest.setCategoryIds(Set.of("cat-001", "cat-002"));
            when(categoryRepository.findAllById(any())).thenReturn(Collections.singletonList(category));

            // When & Then
            assertThatThrownBy(() -> movieService.creatMovie(createRequest))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CATEGORY_NOT_FOUND);
        }
    }

    @Nested
    class UpdateMovieTests {
        @Test
        void updateMovie_Success() {
            // Given
            String id = "movie-001";
            UpdateMovieRequest updateRequest = new UpdateMovieRequest();
            updateRequest.setSlug("dune-updated");
            updateRequest.setAgeRating("C16");
            updateRequest.setCategoryIds(Collections.singleton("cat-001"));

            when(movieRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(movie));
            when(movieRepository.findBySlug("dune-updated")).thenReturn(Optional.empty());
            when(categoryRepository.findAllById(any())).thenReturn(Collections.singletonList(category));
            when(movieRepository.save(any(Movie.class))).thenReturn(movie);
            when(movieMapper.toMovieResponse(any(Movie.class))).thenReturn(movieResponse);

            // When
            MovieResponse result = movieService.updateMovie(id, updateRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(movie.getAgeRating()).isEqualTo(AgeRating.C16);
            verify(movieMapper).updateMovie(movie, updateRequest);
            verify(movieRepository).save(movie);
        }

        @Test
        void updateMovie_SlugSameAsCurrent_NoException() {
            // Given
            String id = "movie-001";
            UpdateMovieRequest updateRequest = new UpdateMovieRequest();
            updateRequest.setSlug("dune-part-two"); // Same slug

            when(movieRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(movie));
            when(movieRepository.findBySlug(any())).thenReturn(Optional.of(movie));
            when(movieRepository.save(any())).thenReturn(movie);
            when(movieMapper.toMovieResponse(any())).thenReturn(movieResponse);

            // When
            movieService.updateMovie(id, updateRequest);

            // Then
            verify(movieRepository).save(movie);
        }

        @Test
        void updateMovie_NotFound_ThrowsException() {
            // Given
            when(movieRepository.findByIdAndDeletedFalse(anyString())).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> movieService.updateMovie("invalid", new UpdateMovieRequest()))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MOVIE_NOT_FOUND);
        }

        @Test
        void updateMovie_DuplicateSlug_ThrowsException() {
            // Given
            String id = "movie-001";
            UpdateMovieRequest updateRequest = new UpdateMovieRequest();
            updateRequest.setSlug("existing-slug");

            Movie anotherMovie = Movie.builder().build();
            anotherMovie.setId("movie-002");

            when(movieRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(movie));
            when(movieRepository.findBySlug("existing-slug")).thenReturn(Optional.of(anotherMovie));

            // When & Then
            assertThatThrownBy(() -> movieService.updateMovie(id, updateRequest))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MOVIE_SLUG_ALREADY_EXISTS);
        }

        @Test
        void updateMovie_CategoryNotFound_ThrowsException() {
            // Given
            String id = "movie-001";
            UpdateMovieRequest updateRequest = new UpdateMovieRequest();
            updateRequest.setCategoryIds(Set.of("cat-001", "cat-999"));

            when(movieRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(movie));
            when(categoryRepository.findAllById(any())).thenReturn(Collections.singletonList(category));

            // When & Then
            assertThatThrownBy(() -> movieService.updateMovie(id, updateRequest))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CATEGORY_NOT_FOUND);
        }

        @Test
        void deleteMovie_Success() {
            // Given
            when(movieRepository.findByIdAndDeletedFalse("movie-001")).thenReturn(Optional.of(movie));
            when(movieRepository.save(any())).thenReturn(movie);

            // When
            movieService.deleteMovie("movie-001");

            // Then
            verify(movieRepository).save(movie);
            // Verify softDelete was called if possible, or just check mocks
        }

        @Test
        void deleteMovie_NotFound_ThrowsException() {
            when(movieRepository.findByIdAndDeletedFalse("unknown")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> movieService.deleteMovie("unknown"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MOVIE_NOT_FOUND);
        }

        @Test
        void updateMovieStatus_Success() {
             // Given
             org.example.cinemaBooking.DTO.Request.Movie.UpdateMovieStatusRequest req = new org.example.cinemaBooking.DTO.Request.Movie.UpdateMovieStatusRequest();
             req.setStatus("ENDED");
             when(movieRepository.findByIdAndDeletedFalse("movie-001")).thenReturn(Optional.of(movie));
             when(movieMapper.toMovieResponse(any())).thenReturn(movieResponse);

             // When
             movieService.updateMovieStatus("movie-001", req);

             // Then
             assertThat(movie.getStatus()).isEqualTo(MovieStatus.ENDED);
             verify(movieRepository).save(movie);
        }
    }

    @Nested
    class GetMovieTests {
        @Test
        void getMovieById_Success() {
            when(movieRepository.findByIdAndDeletedFalse("movie-001")).thenReturn(Optional.of(movie));
            when(movieMapper.toMovieResponse(movie)).thenReturn(movieResponse);

            MovieResponse result = movieService.getMovieById("movie-001");

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("movie-001");
        }

        @Test
        void getMovieById_NotFound_ThrowsException() {
            when(movieRepository.findByIdAndDeletedFalse(any())).thenReturn(Optional.empty());
            assertThatThrownBy(() -> movieService.getMovieById("unknown"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MOVIE_NOT_FOUND);
        }

        @Test
        void getMovieBySlug_Success() {
            when(movieRepository.findBySlugAndDeletedFalse("slug")).thenReturn(Optional.of(movie));
            when(movieMapper.toMovieResponse(movie)).thenReturn(movieResponse);

            MovieResponse result = movieService.getMovieBySlug("slug");

            assertThat(result).isNotNull();
        }

        @Test
        void getMovieBySlug_NotFound_ThrowsException() {
            when(movieRepository.findBySlugAndDeletedFalse(any())).thenReturn(Optional.empty());
            assertThatThrownBy(() -> movieService.getMovieBySlug("unknown"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MOVIE_NOT_FOUND);
        }

        @Test
        void getMovies_WithFilters_Desc_Success() {
            // Given
            int page = 1;
            int size = 10;
            Pageable pageable = PageRequest.of(0, size, Sort.by("title").descending());
            Page<Movie> moviePage = new PageImpl<>(Collections.singletonList(movie), pageable, 1);

            when(movieRepository.findAll((Specification<Movie>) any(), any(Pageable.class))).thenReturn(moviePage);

            // When
            PageResponse<MovieResponse> result = movieService.getMovies(null, null, null, null, page, size, "title",
                    "desc");

            // Then
            assertThat(result).isNotNull();
            verify(movieRepository).findAll(any(Specification.class),
                    argThat((Pageable p) -> p.getSort().getOrderFor("title").getDirection() == Sort.Direction.DESC));
        }

        @Test
        void getMovies_WithFilters_Asc_Success() {
            // Given
            Pageable pageable = PageRequest.of(0, 10, Sort.by("title").ascending());
            when(movieRepository.findAll((Specification<Movie>) any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Collections.singletonList(movie), pageable, 1));

            // When
            movieService.getMovies(null, null, null, null, 1, 10, "title", "asc");

            // Then
            verify(movieRepository).findAll(any(Specification.class),
                    argThat((Pageable p) -> p.getSort().getOrderFor("title").getDirection() == Sort.Direction.ASC));
        }

        @Test
        void getMoviesIsComingSoon_PageZero_Success() {
            // Given
            Pageable pageable = PageRequest.of(0, 10, Sort.by("releaseDate").descending());
            when(movieRepository.findByStatusAndDeletedFalse(eq(MovieStatus.COMING_SOON), any()))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));

            // When
            movieService.getMoviesIsComingSoon(0, 10);

            // Then
            verify(movieRepository).findByStatusAndDeletedFalse(eq(MovieStatus.COMING_SOON),
                    argThat(p -> p.getPageNumber() == 0));
        }

        @Test
        void getMoviesIsComingSoon_PageOne_Success() {
            when(movieRepository.findByStatusAndDeletedFalse(eq(MovieStatus.COMING_SOON), any()))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));
            movieService.getMoviesIsComingSoon(1, 10);
            verify(movieRepository).findByStatusAndDeletedFalse(any(), argThat(p -> p.getPageNumber() == 0));
        }

        @Test
        void getMoviesIsNowShowing_Success() {
            when(movieRepository.findByStatusAndDeletedFalse(eq(MovieStatus.NOW_SHOWING), any()))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));
            movieService.getMoviesIsNowShowing(1, 10);
            verify(movieRepository).findByStatusAndDeletedFalse(eq(MovieStatus.NOW_SHOWING), any());
        }

        @Test
        void getMoviesIsNowShowing_PageZero_Success() {
            when(movieRepository.findByStatusAndDeletedFalse(eq(MovieStatus.NOW_SHOWING), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
            movieService.getMoviesIsNowShowing(0, 10);
            verify(movieRepository).findByStatusAndDeletedFalse(any(), argThat(p -> p.getPageNumber() == 0));
        }

        @Test
        void searchMovie_Success() {
            // Given
            when(movieRepository.searchMovie(anyString(), any())).thenReturn(new PageImpl<>(Collections.emptyList()));

            // When
            movieService.searchMovie(1, 10, "keyword");

            // Then
            verify(movieRepository).searchMovie(eq("keyword"), any());
        }

        @Test
        void searchMovie_PageZero_Success() {
            when(movieRepository.searchMovie(any(), any())).thenReturn(new PageImpl<>(Collections.emptyList()));
            movieService.searchMovie(0, 10, "k");
            verify(movieRepository).searchMovie(any(), argThat(p -> p.getPageNumber() == 0));
        }
    }

    @Nested
    class RecommendationTests {
        @Test
        void recommend_Success() {
            // Given
            MovieStats stat1 = mock(MovieStats.class);
            when(stat1.id()).thenReturn("m1");
            when(stat1.revenue()).thenReturn(1000.0);
            when(stat1.ticketCount()).thenReturn(100L);
            when(stat1.rating()).thenReturn(8.0);

            MovieStats stat2 = mock(MovieStats.class);
            when(stat2.id()).thenReturn("m2");
            when(stat2.revenue()).thenReturn(2000.0);
            when(stat2.ticketCount()).thenReturn(200L);
            when(stat2.rating()).thenReturn(9.0);

            when(movieRepository.getMovieStats()).thenReturn(Arrays.asList(stat1, stat2));

            // When
            List<MovieRecommendResponse> results = movieService.recommend();

            // Then
            assertThat(results).hasSize(2);
            // m2 should have higher score than m1
            assertThat(results.get(0).id()).isEqualTo("m2");
        }

        @Test
        void recommend_EmptyStats_Success() {
            // Given
            when(movieRepository.getMovieStats()).thenReturn(Collections.emptyList());

            // When
            List<MovieRecommendResponse> results = movieService.recommend();

            // Then
            assertThat(results).isEmpty();
        }
    }

    @Nested
    class SchedulerTests {
        @Test
        void updateMovieStatus_Success() {
            // Given
            when(movieRepository.updateStatusToNowShowing(any(), any(), any())).thenReturn(5);

            // When
            movieService.updateMovieStatus();

            // Then
            verify(movieRepository).updateStatusToNowShowing(eq(MovieStatus.NOW_SHOWING), eq(MovieStatus.COMING_SOON),
                    any(LocalDate.class));
        }
    }
}
