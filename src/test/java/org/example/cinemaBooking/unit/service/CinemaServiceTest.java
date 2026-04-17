package org.example.cinemaBooking.unit.service;

import org.example.cinemaBooking.DTO.Request.Cinema.CreateCinemaRequest;
import org.example.cinemaBooking.DTO.Request.Cinema.UpdateCinemaRequest;
import org.example.cinemaBooking.DTO.Response.Cinema.CinemaMovieResponse;
import org.example.cinemaBooking.DTO.Response.Cinema.CinemaResponse;
import org.example.cinemaBooking.DTO.Response.Room.RoomBasicResponse;
import org.example.cinemaBooking.Entity.Cinema;
import org.example.cinemaBooking.Entity.Room;
import org.example.cinemaBooking.Exception.AppException;
import org.example.cinemaBooking.Exception.ErrorCode;
import org.example.cinemaBooking.Mapper.CinemaMapper;
import org.example.cinemaBooking.Mapper.RoomMapper;
import org.example.cinemaBooking.Repository.CinemaRepository;
import org.example.cinemaBooking.Repository.RoomRepository;
import org.example.cinemaBooking.Service.Cinema.CinemaService;
import org.example.cinemaBooking.Shared.enums.Language;
import org.example.cinemaBooking.Shared.enums.RoomType;
import org.example.cinemaBooking.Shared.enums.Status;
import org.example.cinemaBooking.Shared.response.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CinemaServiceTest {

    @Mock
    private CinemaRepository cinemaRepository;
    @Mock
    private CinemaMapper cinemaMapper;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomMapper roomMapper;
    private Cinema cinema;
    private CinemaResponse cinemaResponse;
    private CreateCinemaRequest createRequest;
    private UpdateCinemaRequest updateRequest;
    private Room room;

    @InjectMocks
    private CinemaService cinemaService;

    @BeforeEach
    void setUp() {
        // Setup test data
        cinema = Cinema.builder()
                .name("CGV Vincom Center")
                .address("Vincom Center, Quận 1, TP.HCM")
                .phone("02812345678")
                .hotline("19001234")
                .logoUrl("https://example.com/logo.png")
                .status(Status.ACTIVE)
                .build();
        cinema.setId("cinema-001");
        cinema.setDeleted(false);
        cinemaResponse = CinemaResponse.builder()
                .id("cinema-001")
                .name("CGV Vincom Center")
                .address("Vincom Center, Quận 1, TP.HCM")
                .hotline("19001234")
                .logoUrl("https://example.com/logo.png")
                .phone("02812345678")
                .status(Status.ACTIVE)
                .build();

        createRequest = CreateCinemaRequest.builder()
                .name("CGV Vincom Center")
                .address("Vincom Center, Quận 1, TP.HCM")
                .phone("02812345678")
                .build();

        updateRequest = UpdateCinemaRequest.builder()
                .name("CGV Vincom Center Updated")
                .address("Vincom Center, Quận 1, TP.HCM - Updated")
                .phone("02887654321")
                .build();

        room = Room.builder()
                .name("Phòng 1")
                .cinema(cinema)
                .totalSeats(100)
                .roomType(RoomType.THREE_D)
                .status(Status.ACTIVE)
                .build();
        room.setId("room-001");
    }

    @Nested
    class CreateCinemaTests {
        @Test
        void testCreateCinema_Success() {
            when(cinemaMapper.toCinema(createRequest)).thenReturn(cinema);
            when(cinemaRepository.save(any(Cinema.class))).thenReturn(cinema);
            when(cinemaMapper.toResponse(any(Cinema.class))).thenReturn(cinemaResponse);

            CinemaResponse result = cinemaService.createCinema(createRequest);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo("cinema-001");
            assertThat(result.name()).isEqualTo("CGV Vincom Center");
            assertThat(result.status()).isEqualTo(Status.ACTIVE);
            verify(cinemaRepository).save(any(Cinema.class));
        }

        @Test
        void createCinema_WithNullRequest_ThrowsException() {
            // When & Then
            assertThatThrownBy(() -> cinemaService.createCinema(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class DeleteCinemaTests {
        @Test
        void testDeleteCinema_Success() {
            String id = cinema.getId();
            when(cinemaRepository.findById(id)).thenReturn(Optional.of(cinema));
            cinemaService.deleteCinemaById("cinema-001");
            // Then
            assertThat(cinema.isDeleted()).isTrue();
            verify(cinemaRepository).save(cinema);
        }

        @Test
        void deleteCinema_WithNonExistingId_ThrowsException() {
            // Given
            String nonExistingId = "non-existing-id";
            when(cinemaRepository.findById(nonExistingId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> cinemaService.deleteCinemaById(nonExistingId))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CINEMA_NOT_FOUND);
        }

        @Test
        void deleteCinema_WithAlreadyInactiveCinema_ThrowsException() {
            // Given
            cinema.setStatus(Status.INACTIVE);
            String id = cinema.getId();
            when(cinemaRepository.findById(id)).thenReturn(Optional.of(cinema));

            // When & Then
            assertThatThrownBy(() -> cinemaService.deleteCinemaById(id))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CINEMA_ALREADY_INACTIVE);
        }
    }

    @Nested
    class GetCinemaByIdTests {
        @Test
        void testGetCinemaById_Success() {
            String id = cinema.getId();
            when(cinemaRepository.findById(id)).thenReturn(Optional.of(cinema));
            when(cinemaMapper.toResponse(cinema)).thenReturn(cinemaResponse);

            CinemaResponse result = cinemaService.getCinemaById(id);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo("cinema-001");
            assertThat(result.name()).isEqualTo("CGV Vincom Center");
            verify(cinemaRepository).findById(id);
            verify(cinemaMapper).toResponse(cinema);
        }

        @Test
        void getCinemaById_WithNonExistingId_ThrowsException() {
            // Given
            String nonExistingId = "non-existing-id";
            when(cinemaRepository.findById(nonExistingId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> cinemaService.getCinemaById(nonExistingId))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CINEMA_NOT_FOUND);
        }
    }

    @Nested
    class ToggleCinemaStatusTests {
        @Test
        void toggleCinemaStatus_FromActiveToInactive_Success() {
            // Given
            cinema.setStatus(Status.ACTIVE);
            when(cinemaRepository.findById("cinema-001")).thenReturn(Optional.of(cinema));

            // When
            cinemaService.toggleCinemaStatus("cinema-001");

            // Then
            assertThat(cinema.getStatus()).isEqualTo(Status.INACTIVE);
            verify(cinemaRepository).save(cinema);
        }

        @Test
        void toggleCinemaStatus_FromInactiveToActive_Success() {
            // Given
            cinema.setStatus(Status.INACTIVE);
            when(cinemaRepository.findById("cinema-001")).thenReturn(Optional.of(cinema));

            // When
            cinemaService.toggleCinemaStatus("cinema-001");

            // Then
            assertThat(cinema.getStatus()).isEqualTo(Status.ACTIVE);
            verify(cinemaRepository).save(cinema);
        }

        @Test
        void toggleCinemaStatus_WithNonExistingId_ThrowsException() {
            // Given
            String nonExistingId = "non-existing-id";
            when(cinemaRepository.findById(nonExistingId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> cinemaService.toggleCinemaStatus(nonExistingId))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CINEMA_NOT_FOUND);
        }
    }

    @Nested
    class GetAllCinemasTests {
        @Test
        void testGetAllCinemas_Success() {
            int page = 1;
            int size = 10;
            String sortBy = "name";
            String direction = "asc";
            String keyword = "CGV";
            Cinema cinema2 = Cinema.builder()
                    .name("CGV Aeon Mall")
                    .address("Aeon Mall, Bình Tân, TP.HCM")
                    .phone("02898765432")
                    .hotline("19009876")
                    .logoUrl("https://example.com/logo2.png")
                    .status(Status.ACTIVE)
                    .build();
            cinema2.setId("cinema-002");

            CinemaResponse cinemaResponse2 = CinemaResponse.builder()
                    .id("cinema-002")
                    .name("CGV Aeon Mall")
                    .address("Aeon Mall, Bình Tân, TP.HCM")
                    .hotline("19009876")
                    .logoUrl("https://example.com/logo2.png")
                    .phone("02898765432")
                    .status(Status.ACTIVE)
                    .build();

            Pageable pageable = PageRequest.of(0, size, Sort.by("name").ascending());
            Page<Cinema> cinemaPage = new PageImpl<>(List.of(cinema2, cinema), pageable, 2);
            when(cinemaRepository.findByNameContainingIgnoreCaseAndDeletedFalse(keyword, pageable))
                    .thenReturn(cinemaPage);
            when(cinemaMapper.toResponse(cinema2)).thenReturn(cinemaResponse2);
            when(cinemaMapper.toResponse(cinema)).thenReturn(cinemaResponse);

            var result = cinemaService.getAllCinemas(page, size, sortBy, direction, keyword);

            assertThat(result).isNotNull();
            assertThat(result.getItems()).hasSize(2);
            assertThat(result.getItems().get(0).id()).isEqualTo("cinema-002");
            assertThat(result.getItems().get(1).id()).isEqualTo("cinema-001");
            verify(cinemaRepository).findByNameContainingIgnoreCaseAndDeletedFalse(keyword, pageable);
        }

        @Test
        void getAllCinemas_WithoutKeyword_Success() {
            // Given
            int page = 1;
            int size = 10;
            String sortBy = "name";
            String direction = "asc";
            String keyword = null;

            Pageable pageable = PageRequest.of(0, size, Sort.by("name").ascending());
            Page<Cinema> cinemaPage = new PageImpl<>(List.of(cinema), pageable, 1);

            when(cinemaRepository.findAllByDeletedFalse(any(Pageable.class))).thenReturn(cinemaPage);
            when(cinemaMapper.toResponse(any(Cinema.class))).thenReturn(cinemaResponse);

            // When
            PageResponse<CinemaResponse> result = cinemaService.getAllCinemas(
                    page, size, sortBy, direction, keyword);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getItems()).hasSize(1);
            verify(cinemaRepository).findAllByDeletedFalse(any(Pageable.class));
        }

        @Test
        void getAllCinemas_EmptyResult_Success() {
            // Given
            int page = 1;
            int size = 10;
            String sortBy = "name";
            String direction = "asc";
            String keyword = "Nonexistent";

            Pageable pageable = PageRequest.of(0, size, Sort.by("name").ascending());
            Page<Cinema> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(cinemaRepository.findByNameContainingIgnoreCaseAndDeletedFalse(eq(keyword), any(Pageable.class)))
                    .thenReturn(emptyPage);

            // When
            PageResponse<CinemaResponse> result = cinemaService.getAllCinemas(
                    page, size, sortBy, direction, keyword);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getItems()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
        }

        @Test
        void testGetAllCinemas_Success_Descending() {
            // Given
            int page = 1;
            int size = 10;
            String sortBy = "name";
            String direction = "desc";
            String keyword = "CGV";
            Cinema cinema2 = Cinema.builder()
                    .name("CGV Aeon Mall")
                    .address("Aeon Mall, Bình Tân, TP.HCM")
                    .phone("02898765432")
                    .hotline("19009876")
                    .logoUrl("https://example.com/logo2.png")
                    .status(Status.ACTIVE)
                    .build();
            cinema2.setId("cinema-002");

            CinemaResponse cinemaResponse2 = CinemaResponse.builder()
                    .id("cinema-002")
                    .name("CGV Aeon Mall")
                    .address("Aeon Mall, Bình Tân, TP.HCM")
                    .hotline("19009876")
                    .logoUrl("https://example.com/logo2.png")
                    .phone("02898765432")
                    .status(Status.ACTIVE)
                    .build();

            Pageable pageable = PageRequest.of(0, size, Sort.by("name").descending());
            Page<Cinema> cinemaPage = new PageImpl<>(List.of(cinema, cinema2), pageable, 2);
            when(cinemaRepository.findByNameContainingIgnoreCaseAndDeletedFalse(keyword, pageable))
                    .thenReturn(cinemaPage);
            when(cinemaMapper.toResponse(cinema)).thenReturn(cinemaResponse);
            when(cinemaMapper.toResponse(cinema2)).thenReturn(cinemaResponse2);

            // When
            var result = cinemaService.getAllCinemas(page, size, sortBy, direction, keyword);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getItems()).hasSize(2);
            assertThat(result.getItems().get(0).id()).isEqualTo("cinema-001");
            assertThat(result.getItems().get(1).id()).isEqualTo("cinema-002");
            verify(cinemaRepository).findByNameContainingIgnoreCaseAndDeletedFalse(keyword, pageable);
        }

        @Test
        void getAllCinemas_WithBlankKeyword_Success() {
            // Given
            int page = 1;
            int size = 10;
            String sortBy = "name";
            String direction = "asc";
            String keyword = "   "; // Blank keyword

            Pageable pageable = PageRequest.of(0, size, Sort.by("name").ascending());
            Page<Cinema> cinemaPage = new PageImpl<>(List.of(cinema), pageable, 1);

            when(cinemaRepository.findAllByDeletedFalse(any(Pageable.class))).thenReturn(cinemaPage);
            when(cinemaMapper.toResponse(any(Cinema.class))).thenReturn(cinemaResponse);

            // When
            PageResponse<CinemaResponse> result = cinemaService.getAllCinemas(
                    page, size, sortBy, direction, keyword);

            // Then
            assertThat(result).isNotNull();
            // Should call findAllByDeletedFalse because isBlank() is true
            verify(cinemaRepository).findAllByDeletedFalse(any(Pageable.class));
        }

        @Test
        void getAllCinemas_WithInvalidPage_Success() {
            // Given
            int page = 0; // Invalid page
            int size = 10;
            String direction = "asc";

            Pageable pageable = PageRequest.of(0, size, Sort.by("name").ascending());
            Page<Cinema> cinemaPage = new PageImpl<>(List.of(cinema), pageable, 1);

            when(cinemaRepository.findAllByDeletedFalse(any(Pageable.class))).thenReturn(cinemaPage);
            when(cinemaMapper.toResponse(any(Cinema.class))).thenReturn(cinemaResponse);

            // When
            cinemaService.getAllCinemas(page, size, "name", direction, null);

            // Then
            // Verify it uses page 0 (index 0)
            verify(cinemaRepository).findAllByDeletedFalse(argThat(p -> p.getPageNumber() == 0));
        }
    }

    @Nested
    class UpdateCinemaTests {
        @Test
        void testUpdateCinema_Success() {
            // Given
            String id = cinema.getId();
            CinemaResponse updatedResponse = CinemaResponse.builder()
                    .id(id)
                    .name(updateRequest.name())
                    .address(updateRequest.address())
                    .phone(updateRequest.phone())
                    .status(Status.ACTIVE)
                    .build();

            when(cinemaRepository.findById(id)).thenReturn(Optional.of(cinema));
            when(cinemaRepository.save(any(Cinema.class))).thenReturn(cinema);
            when(cinemaMapper.toResponse(any(Cinema.class))).thenReturn(updatedResponse);

            // When
            CinemaResponse result = cinemaService.updateCinema(id, updateRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(id);
            assertThat(result.name()).isEqualTo(updateRequest.name());
            verify(cinemaRepository).findById(id);
            verify(cinemaMapper).updateCinema(updateRequest, cinema);
            verify(cinemaRepository).save(cinema);
        }

        @Test
        void updateCinema_WithNonExistingId_ThrowsException() {
            // Given
            String nonExistingId = "non-existing-id";
            when(cinemaRepository.findById(nonExistingId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> cinemaService.updateCinema(nonExistingId, updateRequest))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CINEMA_NOT_FOUND);
        }
    }

    @Nested
    class GetMoviesByCinemaAndDateTests {
        @Test
        void getMoviesByCinemaAndDate_Success() {
            // Given
            String cinemaId = "cinema-001";
            LocalDate date = LocalDate.of(2024, 4, 18);
            int page = 1;
            int size = 10;
            String sortBy = "movieTitle";
            String direction = "ASC";

            CinemaMovieResponse movieResponse = new CinemaMovieResponse(
                    "movie-001", "Dune: Part Two", "https://example.com/poster.jpg", 166, Language.ORIGINAL);

            Pageable pageable = PageRequest.of(0, size, Sort.by(Sort.Direction.ASC, sortBy));
            Page<CinemaMovieResponse> moviePage = new PageImpl<>(List.of(movieResponse), pageable, 1);

            LocalDateTime from = date.atStartOfDay();
            LocalDateTime to = from.plusDays(1);

            when(cinemaRepository.findMoviesByCinemaAndDate(eq(cinemaId), eq(from), eq(to), any(Pageable.class)))
                    .thenReturn(moviePage);

            // When
            PageResponse<CinemaMovieResponse> result = cinemaService.getMoviesByCinemaAndDate(
                    cinemaId, date, page, size, sortBy, direction);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getItems().get(0).movieTitle()).isEqualTo("Dune: Part Two");
            verify(cinemaRepository).findMoviesByCinemaAndDate(eq(cinemaId), eq(from), eq(to), any(Pageable.class));
        }

        @Test
        void getMoviesByCinemaAndDate_WithInvalidPage_Success() {
            // Given
            String cinemaId = "cinema-001";
            LocalDate date = LocalDate.of(2024, 4, 18);
            int page = -1; // Invalid page

            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "movieTitle"));
            Page<CinemaMovieResponse> moviePage = new PageImpl<>(List.of(), pageable, 0);

            when(cinemaRepository.findMoviesByCinemaAndDate(any(), any(), any(), any())).thenReturn(moviePage);

            // When
            cinemaService.getMoviesByCinemaAndDate(cinemaId, date, page, 10, "movieTitle", "ASC");

            // Then
            verify(cinemaRepository).findMoviesByCinemaAndDate(eq(cinemaId), any(), any(), argThat(p -> p.getPageNumber() == 0));
        }

        @Test
        void getMoviesByCinemaAndDate_EmptyResult_Success() {
            // Given
            String cinemaId = "cinema-001";
            LocalDate date = LocalDate.of(2024, 4, 18);
            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "movieTitle"));
            Page<CinemaMovieResponse> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(cinemaRepository.findMoviesByCinemaAndDate(eq(cinemaId), any(), any(), any()))
                    .thenReturn(emptyPage);

            // When
            PageResponse<CinemaMovieResponse> result = cinemaService.getMoviesByCinemaAndDate(
                    cinemaId, date, 1, 10, "movieTitle", "ASC");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getItems()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
        }
    }

    @Nested
    class GetRoomsByCinemaTests {
        @Test
        void getRoomsByCinema_Success() {
            // Given
            String cinemaId = "cinema-001";
            int page = 1;
            int size = 10;
            String sortBy = "name";
            String direction = "asc";

            RoomBasicResponse roomResponse = new RoomBasicResponse(
                    "room-001", "Phòng 1", 100, RoomType.THREE_D, Status.ACTIVE, LocalDateTime.now());

            Pageable pageable = PageRequest.of(0, size, Sort.by(Sort.Direction.ASC, sortBy));
            Page<Room> roomPage = new PageImpl<>(List.of(room), pageable, 1);

            when(roomRepository.findByCinemaId(eq(cinemaId), any(Pageable.class))).thenReturn(roomPage);
            when(roomMapper.toBasicResponse(any(Room.class))).thenReturn(roomResponse);

            // When
            PageResponse<RoomBasicResponse> result = cinemaService.getRoomsByCinema(
                    cinemaId, page, size, sortBy, direction);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getItems().get(0).id()).isEqualTo("room-001");
            verify(roomRepository).findByCinemaId(eq(cinemaId), any(Pageable.class));
            verify(roomMapper).toBasicResponse(any(Room.class));
        }

        @Test
        void getRoomsByCinema_WithDescending_Success() {
            // Given
            String cinemaId = "cinema-001";
            String direction = "desc";

            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "name"));
            Page<Room> roomPage = new PageImpl<>(List.of(), pageable, 0);

            when(roomRepository.findByCinemaId(eq(cinemaId), any(Pageable.class))).thenReturn(roomPage);

            // When
            cinemaService.getRoomsByCinema(cinemaId, 1, 10, "name", direction);

            // Then
            verify(roomRepository).findByCinemaId(eq(cinemaId), argThat(p -> p.getSort().getOrderFor("name").isDescending()));
        }

        @Test
        void getRoomsByCinema_WithInvalidPage_Success() {
            // Given
            String cinemaId = "cinema-001";
            int page = 0;

            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "name"));
            Page<Room> roomPage = new PageImpl<>(List.of(), pageable, 0);

            when(roomRepository.findByCinemaId(eq(cinemaId), any(Pageable.class))).thenReturn(roomPage);

            // When
            cinemaService.getRoomsByCinema(cinemaId, page, 10, "name", "asc");

            // Then
            verify(roomRepository).findByCinemaId(eq(cinemaId), argThat(p -> p.getPageNumber() == 0));
        }

        @Test
        void getRoomsByCinema_EmptyResult_Success() {
            // Given
            String cinemaId = "cinema-001";
            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "name"));
            Page<Room> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(roomRepository.findByCinemaId(eq(cinemaId), any(Pageable.class))).thenReturn(emptyPage);

            // When
            PageResponse<RoomBasicResponse> result = cinemaService.getRoomsByCinema(
                    cinemaId, 1, 10, "name", "asc");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getItems()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
        }
    }
}
