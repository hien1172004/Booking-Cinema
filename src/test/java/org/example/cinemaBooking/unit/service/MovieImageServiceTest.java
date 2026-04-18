package org.example.cinemaBooking.unit.service;

import org.example.cinemaBooking.DTO.Request.Movie.CreateMovieImageRequest;
import org.example.cinemaBooking.DTO.Request.Movie.UpdateMovieImageRequest;
import org.example.cinemaBooking.DTO.Response.Movie.MovieImageResponse;
import org.example.cinemaBooking.Entity.Movie;
import org.example.cinemaBooking.Entity.MovieImage;
import org.example.cinemaBooking.Exception.AppException;
import org.example.cinemaBooking.Exception.ErrorCode;
import org.example.cinemaBooking.Mapper.MovieImageMapper;
import org.example.cinemaBooking.Repository.MovieImageRepository;
import org.example.cinemaBooking.Repository.MovieRepository;
import org.example.cinemaBooking.Service.Movie.MovieImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MovieImageServiceTest {

    @Mock private MovieRepository movieRepository;
    @Mock private MovieImageRepository movieImageRepository;
    @Mock private MovieImageMapper movieImageMapper;

    @InjectMocks
    private MovieImageService movieImageService;

    private Movie movie;
    private MovieImage movieImage;

    @BeforeEach
    void setUp() {
        movie = new Movie();
        movie.setId("m-001");

        movieImage = MovieImage.builder()
                .imageUrl("url1")
                .movie(movie)
                .build();
    }

    @Nested
    class ActionTests {
        @Test
        void createMovieImage_Success() {
            // Given
            CreateMovieImageRequest request = new CreateMovieImageRequest(Arrays.asList("url1", "url2"));
            when(movieRepository.findById("m-001")).thenReturn(Optional.of(movie));
            when(movieImageRepository.findByMovieId("m-001")).thenReturn(Collections.singletonList(movieImage));
            when(movieImageRepository.saveAll(any())).thenReturn(Collections.singletonList(new MovieImage()));
            when(movieImageMapper.toResponse(any())).thenReturn(mock(MovieImageResponse.class));

            // When
            List<MovieImageResponse> responses = movieImageService.createMovieImage("m-001", request);

            // Then
            verify(movieImageRepository).saveAll(any());
            assertThat(responses).hasSize(2); // 1 existing + 1 new saved
        }

        @Test
        void createMovieImage_NoNewImages_ReturnsExisting() {
            // Given
            CreateMovieImageRequest request = new CreateMovieImageRequest(Collections.singletonList("url1"));
            when(movieRepository.findById("m-001")).thenReturn(Optional.of(movie));
            when(movieImageRepository.findByMovieId("m-001")).thenReturn(Collections.singletonList(movieImage));
            when(movieImageMapper.toResponse(any())).thenReturn(mock(MovieImageResponse.class));

            // When
            List<MovieImageResponse> responses = movieImageService.createMovieImage("m-001", request);

            // Then
            verify(movieImageRepository, never()).saveAll(any());
            assertThat(responses).hasSize(1);
        }

        @Test
        void updateMovieImage_Success() {
            // Given
            UpdateMovieImageRequest request = new UpdateMovieImageRequest(Arrays.asList("url2", "url3"));
            // url1 is in DB, but not in request -> delete
            // url2, url3 are in request, but not in DB -> add
            when(movieRepository.findById("m-001")).thenReturn(Optional.of(movie));
            when(movieImageRepository.findByMovieId("m-001")).thenReturn(Collections.singletonList(movieImage));

            // When
            movieImageService.updateMovieImage("m-001", request);

            // Then
            verify(movieImageRepository).deleteAllInBatch(any());
            verify(movieImageRepository).saveAll(any());
        }

        @Test
        void deleteMovieImage_Success() {
            when(movieImageRepository.findByIdAndMovieId("img-1", "m-1")).thenReturn(Optional.of(movieImage));
            movieImageService.deleteMovieImage("m-1", "img-1");
            verify(movieImageRepository).delete(movieImage);
        }

        @Test
        void deleteMovieImage_NotFound_ThrowsException() {
            when(movieImageRepository.findByIdAndMovieId(any(), any())).thenReturn(Optional.empty());
            assertThatThrownBy(() -> movieImageService.deleteMovieImage("m", "i"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MOVIE_IMAGE_NOT_FOUND);
        }
    }

    @Nested
    class GetTests {
        @Test
        void getMovieImageByMovieId_Success() {
            when(movieRepository.findById("m-001")).thenReturn(Optional.of(movie));
            when(movieImageRepository.findByMovieId("m-001")).thenReturn(Collections.singletonList(movieImage));
            when(movieImageMapper.toResponse(any())).thenReturn(mock(MovieImageResponse.class));

            List<MovieImageResponse> res = movieImageService.getMovieImageByMovieId("m-001");
            assertThat(res).hasSize(1);
        }
    }
}
