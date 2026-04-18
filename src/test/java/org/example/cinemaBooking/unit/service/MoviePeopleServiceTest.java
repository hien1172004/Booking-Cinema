package org.example.cinemaBooking.unit.service;

import org.example.cinemaBooking.DTO.Request.Movie.AddPeopleToMovieRequest;
import org.example.cinemaBooking.DTO.Request.Movie.PeopleRoleRequest;
import org.example.cinemaBooking.DTO.Request.Movie.UpdateMoviePeopleRequest;
import org.example.cinemaBooking.DTO.Response.Movie.MovieCastResponse;
import org.example.cinemaBooking.DTO.Response.Movie.MoviePeopleResponse;
import org.example.cinemaBooking.Entity.Movie;
import org.example.cinemaBooking.Entity.MoviePeople;
import org.example.cinemaBooking.Entity.People;
import org.example.cinemaBooking.Exception.AppException;
import org.example.cinemaBooking.Exception.ErrorCode;
import org.example.cinemaBooking.Mapper.MoviePeopleMapper;
import org.example.cinemaBooking.Repository.MoviePeopleRepository;
import org.example.cinemaBooking.Repository.MovieRepository;
import org.example.cinemaBooking.Repository.PeopleRepository;
import org.example.cinemaBooking.Service.Movie.MoviePeopleService;
import org.example.cinemaBooking.Shared.enums.MovieRole;
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
public class MoviePeopleServiceTest {

    @Mock private PeopleRepository peopleRepository;
    @Mock private MovieRepository movieRepository;
    @Mock private MoviePeopleRepository moviePeopleRepository;
    @Mock private MoviePeopleMapper moviePeopleMapper;

    @InjectMocks
    private MoviePeopleService moviePeopleService;

    private Movie movie;
    private People people;
    private MoviePeople moviePeople;

    @BeforeEach
    void setUp() {
        movie = new Movie();
        movie.setId("m-001");
        movie.setTitle("Inception");

        people = new People();
        people.setId("p-001");
        people.setName("Leo");

        moviePeople = MoviePeople.builder()
                .movie(movie)
                .people(people)
                .movieRole(MovieRole.ACTOR)
                .build();
    }

    @Nested
    class ActionTests {
        @Test
        void addPeopleToMovie_Success() {
            // Given
            PeopleRoleRequest pr = new PeopleRoleRequest("p-001", "ACTOR");
            AddPeopleToMovieRequest request = new AddPeopleToMovieRequest(Collections.singletonList(pr));

            when(movieRepository.findById("m-001")).thenReturn(Optional.of(movie));
            when(moviePeopleRepository.findPeopleIdsByMovieId("m-001")).thenReturn(new HashSet<>());
            when(peopleRepository.countByIdIn(any())).thenReturn(1L);
            when(peopleRepository.findAllById(any())).thenReturn(Collections.singletonList(people));
            when(moviePeopleRepository.saveAll(any())).thenReturn(Collections.singletonList(moviePeople));
            when(moviePeopleRepository.findByMovieId("m-001")).thenReturn(Collections.singletonList(moviePeople));
            when(moviePeopleMapper.toMoviePeopleResponse(any())).thenReturn(mock(MoviePeopleResponse.class));

            // When
            moviePeopleService.addPeopleToMovie("m-001", request);

            // Then
            verify(moviePeopleRepository).saveAll(any());
        }

        @Test
        void addPeopleToMovie_DirectorConflict_ThrowsException() {
            // Given
            PeopleRoleRequest pr = new PeopleRoleRequest("p-002", "DIRECTOR");
            AddPeopleToMovieRequest request = new AddPeopleToMovieRequest(Collections.singletonList(pr));

            when(movieRepository.findById("m-001")).thenReturn(Optional.of(movie));
            when(moviePeopleRepository.findPeopleIdsByMovieId("m-001")).thenReturn(new HashSet<>());
            when(peopleRepository.countByIdIn(any())).thenReturn(1L);
            when(moviePeopleRepository.countByMovieIdAndMovieRole("m-001", MovieRole.DIRECTOR)).thenReturn(1L);

            // When & Then
            assertThatThrownBy(() -> moviePeopleService.addPeopleToMovie("m-001", request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MOVIE_CANNOT_HAVE_MULTIPLE_DIRECTORS);
        }

        @Test
        void addPeopleToMovie_AllExisting_EarlyReturn() {
            PeopleRoleRequest pr = new PeopleRoleRequest("p-001", "ACTOR");
            AddPeopleToMovieRequest request = new AddPeopleToMovieRequest(Collections.singletonList(pr));

            when(movieRepository.findById("m-001")).thenReturn(Optional.of(movie));
            when(moviePeopleRepository.findPeopleIdsByMovieId("m-001")).thenReturn(new HashSet<>(Collections.singletonList("p-001"))); // Already in movie
            when(peopleRepository.countByIdIn(any())).thenReturn(1L); // Pass validation

            moviePeopleService.addPeopleToMovie("m-001", request);
            
            // Should early return and NOT call saveAll
            verify(moviePeopleRepository, never()).saveAll(anyList());
        }

        @Test
        void addPeopleToMovie_EmptyRequest_ThrowsException() {
            AddPeopleToMovieRequest request = new AddPeopleToMovieRequest(Collections.emptyList());
            when(movieRepository.findById("m-001")).thenReturn(Optional.of(movie));
            
            assertThatThrownBy(() -> moviePeopleService.addPeopleToMovie("m-001", request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST);
        }

        @Test
        void addPeopleToMovie_DuplicatePeople_ThrowsException() {
            PeopleRoleRequest pr1 = new PeopleRoleRequest("p-001", "ACTOR");
            PeopleRoleRequest pr2 = new PeopleRoleRequest("p-001", "DIRECTOR");
            AddPeopleToMovieRequest request = new AddPeopleToMovieRequest(Arrays.asList(pr1, pr2));

            when(movieRepository.findById("m-001")).thenReturn(Optional.of(movie));
            when(moviePeopleRepository.findPeopleIdsByMovieId("m-001")).thenReturn(new HashSet<>());

            assertThatThrownBy(() -> moviePeopleService.addPeopleToMovie("m-001", request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_PEOPLE_IN_REQUEST);
        }

        @Test
        void addPeopleToMovie_PeopleNotFound_ThrowsException() {
            PeopleRoleRequest pr = new PeopleRoleRequest("p-unknown", "ACTOR");
            AddPeopleToMovieRequest request = new AddPeopleToMovieRequest(Collections.singletonList(pr));

            when(movieRepository.findById("m-001")).thenReturn(Optional.of(movie));
            when(moviePeopleRepository.findPeopleIdsByMovieId("m-001")).thenReturn(new HashSet<>());
            when(peopleRepository.countByIdIn(any())).thenReturn(0L); // Not found

            assertThatThrownBy(() -> moviePeopleService.addPeopleToMovie("m-001", request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PEOPLE_NOT_FOUND);
        }

        @Test
        void addPeopleToMovie_InvalidRole_ThrowsException() {
            PeopleRoleRequest pr = new PeopleRoleRequest("p-001", "INVALID_ROLE");
            AddPeopleToMovieRequest request = new AddPeopleToMovieRequest(Collections.singletonList(pr));

            when(movieRepository.findById("m-001")).thenReturn(Optional.of(movie));
            when(moviePeopleRepository.findPeopleIdsByMovieId("m-001")).thenReturn(new HashSet<>());
            when(peopleRepository.countByIdIn(any())).thenReturn(1L);

            assertThatThrownBy(() -> moviePeopleService.addPeopleToMovie("m-001", request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_MOVIE_ROLE);
        }

        @Test
        void updateMoviePeople_Replace_Success() {
            // Given
            PeopleRoleRequest pr = new PeopleRoleRequest("p-001", "DIRECTOR");
            UpdateMoviePeopleRequest request = new UpdateMoviePeopleRequest(Collections.singletonList(pr));

            when(movieRepository.findById("m-001")).thenReturn(Optional.of(movie));
            when(moviePeopleRepository.findByMovieId("m-001")).thenReturn(Collections.singletonList(moviePeople));
            when(peopleRepository.countByIdIn(any())).thenReturn(1L);

            // When
            moviePeopleService.updateMoviePeople("m-001", request);

            // Then
            assertThat(moviePeople.getMovieRole()).isEqualTo(MovieRole.DIRECTOR);
            verify(moviePeopleRepository).saveAll(any());
        }

        @Test
        void updateMoviePeople_AddDeleteAndConflict_Success() {
            // Request has "p-002" -> new, "p-001" is dropped -> deleted
            PeopleRoleRequest pr = new PeopleRoleRequest("p-002", "ACTOR");
            UpdateMoviePeopleRequest request = new UpdateMoviePeopleRequest(Collections.singletonList(pr));

            when(movieRepository.findById("m-001")).thenReturn(Optional.of(movie));
            when(moviePeopleRepository.findByMovieId("m-001")).thenReturn(Collections.singletonList(moviePeople)); // "p-001"
            when(peopleRepository.countByIdIn(any())).thenReturn(1L);
            
            // For new people
            People p2 = new People(); p2.setId("p-002");
            when(peopleRepository.findAllById(any())).thenReturn(Collections.singletonList(p2));

            moviePeopleService.updateMoviePeople("m-001", request);

            // verify delete old
            verify(moviePeopleRepository).deleteAllInBatch(anyList());
            // verify save new
            verify(moviePeopleRepository).saveAll(argThat(list -> {
                int count = 0;
                for (Object item : list) count++;
                return count > 0;
            }));
        }

        @Test
        void updateMoviePeople_DirectorConflict_ThrowsException() {
            // "p-002" wants to be DIRECTOR, but "p-001" is already DIRECTOR
            moviePeople.setMovieRole(MovieRole.DIRECTOR); // Existing is director
            PeopleRoleRequest pr = new PeopleRoleRequest("p-002", "DIRECTOR");
            UpdateMoviePeopleRequest request = new UpdateMoviePeopleRequest(Collections.singletonList(pr));

            when(movieRepository.findById("m-001")).thenReturn(Optional.of(movie));
            when(moviePeopleRepository.findByMovieId("m-001")).thenReturn(Collections.singletonList(moviePeople)); // p-001
            when(peopleRepository.countByIdIn(any())).thenReturn(1L);
            when(moviePeopleRepository.countByMovieIdAndMovieRole("m-001", MovieRole.DIRECTOR)).thenReturn(1L);

            assertThatThrownBy(() -> moviePeopleService.updateMoviePeople("m-001", request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MOVIE_ALREADY_HAS_DIRECTOR);
        }

        @Test
        void removePeopleFromMovie_Success() {
            when(moviePeopleRepository.deleteByMovieIdAndPeopleId("m-001", "p-001")).thenReturn(1);
            moviePeopleService.removePeopleFromMovie("m-001", "p-001");
            verify(moviePeopleRepository).deleteByMovieIdAndPeopleId("m-001", "p-001");
        }

        @Test
        void removePeopleFromMovie_NotFound_ThrowsException() {
            when(moviePeopleRepository.deleteByMovieIdAndPeopleId(any(), any())).thenReturn(0);
            assertThatThrownBy(() -> moviePeopleService.removePeopleFromMovie("m", "p"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MOVIE_PEOPLE_NOT_FOUND);
        }

        @Test
        void removeMultiplePeopleFromMovie_Success() {
            moviePeopleService.removeMultiplePeopleFromMovie("m-001", Collections.singletonList("p-001"));
            verify(moviePeopleRepository).deleteByMovieIdAndPeopleId("m-001", "p-001");
        }

        @Test
        void removeAllPeopleFromMovie_Success() {
            moviePeopleService.removeAllPeopleFromMovie("m-001");
            verify(moviePeopleRepository).deleteByMovieId("m-001");
        }
    }

    @Nested
    class ListTests {
        @Test
        void getPeopleByMovie_Success() {
            when(movieRepository.existsById("m-001")).thenReturn(true);
            when(moviePeopleRepository.findByMovieId("m-001")).thenReturn(Collections.singletonList(moviePeople));
            when(moviePeopleMapper.toMovieCastResponse(any())).thenReturn(mock(MovieCastResponse.class));

            List<MovieCastResponse> res = moviePeopleService.getPeopleByMovie("m-001");
            assertThat(res).hasSize(1);
        }

        @Test
        void getPeopleByMovie_NotFound_ThrowsException() {
            when(movieRepository.existsById(any())).thenReturn(false);
            assertThatThrownBy(() -> moviePeopleService.getPeopleByMovie("unknown"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MOVIE_NOT_FOUND);
        }
    }
}
