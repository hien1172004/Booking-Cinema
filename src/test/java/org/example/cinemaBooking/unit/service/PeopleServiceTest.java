package org.example.cinemaBooking.unit.service;

import org.example.cinemaBooking.DTO.Request.Movie.CreatePeopleRequest;
import org.example.cinemaBooking.DTO.Request.Movie.UpdatePeopleRequest;
import org.example.cinemaBooking.DTO.Response.Movie.PeopleResponse;
import org.example.cinemaBooking.Entity.People;
import org.example.cinemaBooking.Exception.AppException;
import org.example.cinemaBooking.Exception.ErrorCode;
import org.example.cinemaBooking.Mapper.PeopleMapper;
import org.example.cinemaBooking.Repository.MoviePeopleRepository;
import org.example.cinemaBooking.Repository.PeopleRepository;
import org.example.cinemaBooking.Service.Movie.PeopleService;
import org.example.cinemaBooking.Shared.response.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PeopleServiceTest {

    @Mock
    private PeopleRepository peopleRepository;
    @Mock
    private MoviePeopleRepository moviePeopleRepository;
    @Mock
    private PeopleMapper peopleMapper;
    @Mock
    private org.example.cinemaBooking.Mapper.MoviePeopleMapper moviePeopleMapper;

    @InjectMocks
    private PeopleService peopleService;

    private People people;
    private CreatePeopleRequest createRequest;
    private UpdatePeopleRequest updateRequest;

    @BeforeEach
    void setUp() {
        people = new People();
        people.setId("p-001");
        people.setName("Christopher Nolan");
        people.setNation("UK");
        people.setDob(LocalDate.of(1970, 7, 30));
        people.setAvatarUrl("url");

        createRequest = new CreatePeopleRequest("Christopher Nolan", "UK", "url", LocalDate.of(1970, 7, 30));
        updateRequest = new UpdatePeopleRequest("Christopher Nolan Updated", "UK", "url", LocalDate.of(1970, 7, 30));
    }

    @Nested
    class ActionTests {
        @Test
        void createPeople_Success() {
            when(peopleMapper.toEntity(any())).thenReturn(people);
            when(peopleRepository.save(any())).thenReturn(people);
            when(peopleMapper.toResponse(any())).thenReturn(mock(PeopleResponse.class));

            peopleService.createPeople(createRequest);
            verify(peopleRepository).save(any());
        }

        @Test
        void updatePeople_Success() {
            when(peopleRepository.findById("p-001")).thenReturn(Optional.of(people));
            when(peopleRepository.save(any())).thenReturn(people);
            when(peopleMapper.toResponse(any())).thenReturn(mock(PeopleResponse.class));

            peopleService.updatePeople("p-001", updateRequest);
            verify(peopleMapper).updatePeople(eq(updateRequest), eq(people));
            verify(peopleRepository).save(people);
        }

        @Test
        void updatePeople_NotFound_ThrowsException() {
            when(peopleRepository.findById("invalid")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> peopleService.updatePeople("invalid", updateRequest))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PEOPLE_NOT_FOUND);
        }

        @Test
        void deletePeople_Success() {
            when(peopleRepository.findById("p-001")).thenReturn(Optional.of(people));
            peopleService.deletePeople("p-001");
            verify(moviePeopleRepository).deleteByPeopleId("p-001");
            verify(peopleRepository).delete(people);
        }

        @Test
        void deletePeople_NotFound_ThrowsException() {
            when(peopleRepository.findById(any())).thenReturn(Optional.empty());
            assertThatThrownBy(() -> peopleService.deletePeople("unknown"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PEOPLE_NOT_FOUND);
        }
    }

    @Nested
    class ListTests {
        @Test
        void getAllPeoples_Success() {
            Page<People> page = new PageImpl<>(Collections.singletonList(people));
            when(peopleRepository.findAll(any(Pageable.class))).thenReturn(page);
            when(peopleMapper.toResponse(any())).thenReturn(mock(PeopleResponse.class));

            PageResponse<PeopleResponse> response = peopleService.getAllPeoples(1, 10, null);
            assertThat(response.getItems()).hasSize(1);
        }

        @Test
        void getAllPeoples_WithKeyword_Success() {
            Page<People> page = new PageImpl<>(Collections.singletonList(people));
            when(peopleRepository.findByNameContainingIgnoreCase(any(), any())).thenReturn(page);
            when(peopleMapper.toResponse(any())).thenReturn(mock(PeopleResponse.class));

            PageResponse<PeopleResponse> response = peopleService.getAllPeoples(1, 10, "keyword");
            assertThat(response.getItems()).hasSize(1);
        }

        @Test
        void getPeopleById_Success() {
            when(peopleRepository.findById("p-001")).thenReturn(Optional.of(people));
            when(peopleMapper.toResponse(people)).thenReturn(mock(PeopleResponse.class));

            PeopleResponse response = peopleService.getPeopleById("p-001");
            assertThat(response).isNotNull();
        }

        @Test
        void getPeopleById_NotFound_ThrowsException() {
            when(peopleRepository.findById("invalid")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> peopleService.getPeopleById("invalid"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PEOPLE_NOT_FOUND);
        }

        @Test
        void getMoviesByPeople_Success() {
            when(peopleRepository.existsById("p-001")).thenReturn(true);
            when(moviePeopleRepository.findByPeopleId("p-001")).thenReturn(Collections.singletonList(mock(org.example.cinemaBooking.Entity.MoviePeople.class)));
            when(moviePeopleMapper.toMoviePeopleResponse(any())).thenReturn(mock(org.example.cinemaBooking.DTO.Response.Movie.MoviePeopleResponse.class));

            java.util.List<org.example.cinemaBooking.DTO.Response.Movie.MoviePeopleResponse> responses = peopleService.getMoviesByPeople("p-001");
            assertThat(responses).hasSize(1);
        }

        @Test
        void getMoviesByPeople_NotFound_ThrowsException() {
            when(peopleRepository.existsById("invalid")).thenReturn(false);
            assertThatThrownBy(() -> peopleService.getMoviesByPeople("invalid"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PEOPLE_NOT_FOUND);
        }
    }
}
