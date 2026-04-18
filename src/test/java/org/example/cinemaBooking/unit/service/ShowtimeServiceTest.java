package org.example.cinemaBooking.unit.service;

import org.example.cinemaBooking.DTO.Request.Showtime.CreateShowtimeRequest;
import org.example.cinemaBooking.DTO.Request.Showtime.UpdateShowtimeRequest;
import org.example.cinemaBooking.DTO.Request.Showtime.ShowtimeFilterRequest;
import org.example.cinemaBooking.DTO.Response.Showtime.ShowtimeDetailResponse;
import org.example.cinemaBooking.DTO.Response.Showtime.ShowtimeSummaryResponse;
import org.example.cinemaBooking.Entity.*;
import org.example.cinemaBooking.Exception.AppException;
import org.example.cinemaBooking.Exception.ErrorCode;
import org.example.cinemaBooking.Mapper.ShowtimeMapper;
import org.example.cinemaBooking.Repository.MovieRepository;
import org.example.cinemaBooking.Repository.RoomRepository;
import org.example.cinemaBooking.Repository.ShowtimeRepository;
import org.example.cinemaBooking.Service.Showtime.ShowtimeService;
import org.example.cinemaBooking.Shared.enums.SeatStatus;
import org.example.cinemaBooking.Shared.enums.ShowTimeStatus;
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
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShowtimeServiceTest {

    @Mock
    private ShowtimeRepository showtimeRepository;
    @Mock
    private ShowtimeMapper showtimeMapper;
    @Mock
    private MovieRepository movieRepository;
    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private ShowtimeService showtimeService;

    private Movie movie;
    private Room room;
    private Seat seat;
    private Showtime showtime;
    private CreateShowtimeRequest createRequest;
    private ShowtimeDetailResponse detailResponse;

    @BeforeEach
    void setUp() {
        movie = Movie.builder()
                .title("Dune")
                .duration(120)
                .build();
        movie.setId("movie-001");

        seat = Seat.builder().active(true).build();
        seat.setId("seat-001");

        room = Room.builder()
                .name("Room A")
                .seats(new ArrayList<>(Collections.singletonList(seat)))
                .build();
        room.setId("room-001");

        showtime = Showtime.builder()
                .startTime(LocalDateTime.now().plusDays(1))
                .status(ShowTimeStatus.SCHEDULED)
                .movie(movie)
                .room(room)
                .showtimeSeats(new ArrayList<>())
                .build();
        showtime.setId("st-001");

        createRequest = new CreateShowtimeRequest(
                "movie-001", "room-001", LocalDateTime.now().plusDays(1),
                BigDecimal.valueOf(100000), "ORIGINAL"
        );

        detailResponse = mock(ShowtimeDetailResponse.class);
    }

    @Nested
    class CreateShowtimeTests {
        @Test
        void createShowtime_Success() {
            // Given
            when(roomRepository.findByIdWithSeats("room-001")).thenReturn(Optional.of(room));
            when(movieRepository.findById("movie-001")).thenReturn(Optional.of(movie));
            when(showtimeRepository.existsConflict(eq("room-001"), eq("0"), any(), any())).thenReturn(false);
            when(showtimeMapper.toEntity(createRequest)).thenReturn(showtime);
            when(showtimeRepository.save(any(Showtime.class))).thenReturn(showtime);
            when(showtimeMapper.toDetailResponse(any(Showtime.class))).thenReturn(detailResponse);

            // When
            ShowtimeDetailResponse result = showtimeService.createShowtime(createRequest);

            // Then
            assertThat(result).isNotNull();
            // Verify seats are generated (1 seat from room)
            assertThat(showtime.getShowtimeSeats()).hasSize(1);
            assertThat(showtime.getShowtimeSeats().get(0).getStatus()).isEqualTo(SeatStatus.AVAILABLE);
            verify(showtimeRepository).save(showtime);
        }

        @Test
        void createShowtime_Conflict_ThrowsException() {
            // Given
            when(roomRepository.findByIdWithSeats("room-001")).thenReturn(Optional.of(room));
            when(movieRepository.findById("movie-001")).thenReturn(Optional.of(movie));
            when(showtimeRepository.existsConflict(any(), any(), any(), any())).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> showtimeService.createShowtime(createRequest))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SHOWTIME_CONFLICT);
        }
    }

    @Nested
    class UpdateShowtimeTests {
        @Test
        void updateShowtime_Success() {
            // Given
            UpdateShowtimeRequest updateRequest = new UpdateShowtimeRequest(null, null, BigDecimal.valueOf(120000), null, null);
            when(showtimeRepository.findByIdWithDetails("st-001")).thenReturn(Optional.of(showtime));
            when(showtimeRepository.save(any(Showtime.class))).thenReturn(showtime);
            when(showtimeMapper.toDetailResponse(any(Showtime.class))).thenReturn(detailResponse);

            // When
            showtimeService.updateShowtime("st-001", updateRequest);

            // Then
            verify(showtimeMapper).updateEntityFromRequest(updateRequest, showtime);
            verify(showtimeRepository).save(showtime);
        }

        @Test
        void updateShowtime_StateInvalid_ThrowsException() {
            // Given
            showtime.setStatus(ShowTimeStatus.FINISHED);
            UpdateShowtimeRequest updateRequest = new UpdateShowtimeRequest(null, null, null, null, null);
            when(showtimeRepository.findByIdWithDetails("st-001")).thenReturn(Optional.of(showtime));

            // When & Then
            assertThatThrownBy(() -> showtimeService.updateShowtime("st-001", updateRequest))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SHOWTIME_STATE_INVALID);
        }

        @Test
        void updateShowtime_ChangeRoom_Success() {
            Room newRoom = Room.builder().name("Room B").seats(new ArrayList<>(Collections.singletonList(seat))).build();
            newRoom.setId("room-002");
            UpdateShowtimeRequest request = new UpdateShowtimeRequest("room-002", null, null, null, null);

            when(showtimeRepository.findByIdWithDetails("st-001")).thenReturn(Optional.of(showtime));
            when(roomRepository.findByIdWithSeats("room-002")).thenReturn(Optional.of(newRoom));
            when(showtimeRepository.save(any(Showtime.class))).thenReturn(showtime);
            when(showtimeMapper.toDetailResponse(any(Showtime.class))).thenReturn(detailResponse);

            ShowtimeDetailResponse result = showtimeService.updateShowtime("st-001", request);

            assertThat(showtime.getRoom().getId()).isEqualTo("room-002");
            assertThat(showtime.getShowtimeSeats()).hasSize(1);
            verify(showtimeRepository).save(any(Showtime.class));
        }

        @Test
        void updateShowtime_Conflict_ThrowsException() {
            UpdateShowtimeRequest request = new UpdateShowtimeRequest(null, LocalDateTime.now().plusDays(2), null, null, null);

            when(showtimeRepository.findByIdWithDetails("st-001")).thenReturn(Optional.of(showtime));
            when(showtimeRepository.existsConflict(eq("room-001"), eq("st-001"), any(), any())).thenReturn(true);

            assertThatThrownBy(() -> showtimeService.updateShowtime("st-001", request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SHOWTIME_CONFLICT);
        }
    }

    @Nested
    class CancelAndDeleteTests {
        @Test
        void cancelShowtime_Success() {
            // Given
            ShowtimeSeat ss = ShowtimeSeat.builder().status(SeatStatus.LOCKED).build();
            showtime.getShowtimeSeats().add(ss);
            when(showtimeRepository.findByIdWithDetails("st-001")).thenReturn(Optional.of(showtime));
            when(showtimeRepository.save(any(Showtime.class))).thenReturn(showtime);

            // When
            showtimeService.cancelShowtime("st-001");

            // Then
            assertThat(showtime.getStatus()).isEqualTo(ShowTimeStatus.CANCELLED);
            assertThat(ss.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
            verify(showtimeRepository).save(showtime);
        }

        @Test
        void cancelShowtime_AlreadyFinished_ThrowsException() {
            showtime.setStatus(ShowTimeStatus.FINISHED);
            when(showtimeRepository.findByIdWithDetails("st-001")).thenReturn(Optional.of(showtime));

            assertThatThrownBy(() -> showtimeService.cancelShowtime("st-001"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SHOWTIME_STATE_INVALID);
        }

        @Test
        void deleteShowtime_Success() {
            // Given
            when(showtimeRepository.findByIdWithDetails("st-001")).thenReturn(Optional.of(showtime));

            // When
            showtimeService.deleteShowtime("st-001");

            // Then
            assertThat(showtime.isDeleted()).isTrue();
            verify(showtimeRepository).save(showtime);
        }
    }

    @Nested
    class SchedulerTests {
        @Test
        void startDueShowtimes_Success() {
            // Given
            when(showtimeRepository.findScheduledShowtimesToStart(any())).thenReturn(Collections.singletonList(showtime));

            // When
            showtimeService.startDueShowtimes();

            // Then
            assertThat(showtime.getStatus()).isEqualTo(ShowTimeStatus.ONGOING);
            verify(showtimeRepository).saveAll(anyList());
        }

        @Test
        void finishEndedShowtimes_Success() {
            // Given
            showtime.setStatus(ShowTimeStatus.ONGOING);
            showtime.setStartTime(LocalDateTime.now().minusHours(5)); // definitely finished
            
            when(showtimeRepository.findAllOngoing()).thenReturn(Collections.singletonList(showtime));

            // When
            showtimeService.finishEndedShowtimes();

            // Then
            assertThat(showtime.getStatus()).isEqualTo(ShowTimeStatus.FINISHED);
            verify(showtimeRepository).saveAll(anyList());
        }

        @Test
        void startDueShowtimes_Empty_DoesNothing() {
            when(showtimeRepository.findScheduledShowtimesToStart(any())).thenReturn(Collections.emptyList());
            showtimeService.startDueShowtimes();
            verify(showtimeRepository, never()).saveAll(anyList());
        }

        @Test
        void finishEndedShowtimes_Empty_DoesNothing() {
            when(showtimeRepository.findAllOngoing()).thenReturn(Collections.emptyList());
            showtimeService.finishEndedShowtimes();
            verify(showtimeRepository, never()).saveAll(anyList());
        }
    }

    @Nested
    class ListTests {
        @Test
        void getShowtime_Success() {
            // Given
            ShowtimeFilterRequest filter = new ShowtimeFilterRequest(null, null, null, null, null, null, null, 1, 10);
            Page<Showtime> page = new PageImpl<>(Collections.singletonList(showtime));
            when(showtimeRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

            // When
            PageResponse<ShowtimeSummaryResponse> result = showtimeService.getShowtime(filter);

            // Then
            assertThat(result).isNotNull();
            verify(showtimeRepository).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        void getShowtimeByMovieAndDate_Success() {
            // Given
            when(showtimeRepository.findByMovieAndDateRange(any(), any(), any(), any()))
                    .thenReturn(Collections.singletonList(showtime));

            // When
            List<ShowtimeSummaryResponse> result = showtimeService.getShowtimeByMovieAndDate("m1", LocalDate.now());

            // Then
            assertThat(result).isNotNull();
            verify(showtimeRepository).findByMovieAndDateRange(eq("m1"), any(), any(), eq(ShowTimeStatus.CANCELLED));
        }

        @Test
        void getShowtimeById_Success() {
            when(showtimeRepository.findByIdWithDetails("st-001")).thenReturn(Optional.of(showtime));
            when(showtimeMapper.toDetailResponse(showtime)).thenReturn(detailResponse);

            ShowtimeDetailResponse response = showtimeService.getShowtimeById("st-001");
            assertThat(response).isNotNull();
        }

        @Test
        void getShowtimesByCinemaAndDate_Success() {
            when(showtimeRepository.findByCinemaAndDateRange(any(), any(), any(), any()))
                    .thenReturn(Collections.singletonList(showtime));
            
            List<ShowtimeSummaryResponse> result = showtimeService.getShowtimesByCinemaAndDate("cin-001", LocalDate.now());
            assertThat(result).isNotNull();
        }
    }
}
