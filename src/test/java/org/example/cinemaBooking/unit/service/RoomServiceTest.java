package org.example.cinemaBooking.unit.service;

import org.example.cinemaBooking.DTO.Request.Room.CreateRoomRequest;
import org.example.cinemaBooking.DTO.Request.Room.UpdateRoomRequest;
import org.example.cinemaBooking.DTO.Response.Room.RoomResponse;
import org.example.cinemaBooking.Entity.Cinema;
import org.example.cinemaBooking.Entity.Room;
import org.example.cinemaBooking.Exception.AppException;
import org.example.cinemaBooking.Exception.ErrorCode;
import org.example.cinemaBooking.Mapper.RoomMapper;
import org.example.cinemaBooking.Repository.CinemaRepository;
import org.example.cinemaBooking.Repository.RoomRepository;
import org.example.cinemaBooking.Service.Room.RoomService;
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

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RoomServiceTest {

    @Mock
    private RoomMapper roomMapper;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private CinemaRepository cinemaRepository;

    @InjectMocks
    private RoomService roomService;

    private Room room;
    private Cinema cinema;
    private CreateRoomRequest createRequest;
    private RoomResponse roomResponse;

    @BeforeEach
    void setUp() {
        cinema = Cinema.builder()
                .name("CGV Test")
                .build();
        cinema.setId("cinema-001");

        room = Room.builder()
                .name("Room 1")
                .totalSeats(100)
                .roomType(RoomType.THREE_D)
                .status(Status.ACTIVE)
                .cinema(cinema)
                .build();
        room.setId("room-001");

        createRequest = new CreateRoomRequest("Room 1", 100, "THREE_D", "cinema-001");

        roomResponse = new RoomResponse(
                "room-001", "Room 1", 100, RoomType.THREE_D, Status.ACTIVE,
                "cinema-001", "CGV Test", LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Nested
    class CreateRoomTests {
        @Test
        void createRoom_Success() {
            // Given
            when(roomMapper.toRoomEntity(any(CreateRoomRequest.class))).thenReturn(room);
            when(cinemaRepository.findCinemaById("cinema-001")).thenReturn(cinema);
            when(roomRepository.save(any(Room.class))).thenReturn(room);
            when(roomMapper.toResponse(any(Room.class))).thenReturn(roomResponse);

            // When
            RoomResponse result = roomService.createRoom(createRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo("room-001");
            verify(roomRepository).save(any(Room.class));
        }
    }

    @Nested
    class DeleteRoomTests {
        @Test
        void deleteRoom_Success() {
            // Given
            when(roomRepository.findById("room-001")).thenReturn(Optional.of(room));

            // When
            roomService.deleteRoomByID("room-001");

            // Then
            verify(roomRepository).delete(room);
        }

        @Test
        void deleteRoom_NotFound_ThrowsException() {
            // Given
            when(roomRepository.findById("invalid")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> roomService.deleteRoomByID("invalid"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_NOT_FOUND);
        }

        @Test
        void deleteRoom_AlreadyInactive_ThrowsException() {
            // Given
            room.setStatus(Status.INACTIVE);
            when(roomRepository.findById("room-001")).thenReturn(Optional.of(room));

            // When & Then
            assertThatThrownBy(() -> roomService.deleteRoomByID("room-001"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_ALREADY_INACTIVE);
        }
    }

    @Nested
    class GetRoomTests {
        @Test
        void getRoomByID_Success() {
            // Given
            when(roomRepository.findById("room-001")).thenReturn(Optional.of(room));
            when(roomMapper.toResponse(room)).thenReturn(roomResponse);

            // When
            RoomResponse result = roomService.getRoomByID("room-001");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("Room 1");
        }

        @Test
        void toggleRoomStatus_ToInactive_Success() {
            // Given
            room.setStatus(Status.ACTIVE);
            when(roomRepository.findById("room-001")).thenReturn(Optional.of(room));

            // When
            roomService.toggleRoomStatus("room-001");

            // Then
            assertThat(room.getStatus()).isEqualTo(Status.INACTIVE);
        }

        @Test
        void toggleRoomStatus_ToActive_Success() {
            // Given
            room.setStatus(Status.INACTIVE);
            when(roomRepository.findById("room-001")).thenReturn(Optional.of(room));

            // When
            roomService.toggleRoomStatus("room-001");

            // Then
            assertThat(room.getStatus()).isEqualTo(Status.ACTIVE);
        }

        @Test
        void getRoomByID_NotFound_ThrowsException() {
            when(roomRepository.findById("invalid")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> roomService.getRoomByID("invalid"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_NOT_FOUND);
        }

        @Test
        void toggleRoomStatus_NotFound_ThrowsException() {
            when(roomRepository.findById("invalid")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> roomService.toggleRoomStatus("invalid"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_NOT_FOUND);
        }
    }

    @Nested
    class RoomListTests {
        @Test
        void getAllRooms_WithoutKeyword_Success() {
            // Given
            Pageable pageable = PageRequest.of(0, 10, Sort.by("name").ascending());
            Page<Room> roomPage = new PageImpl<>(Collections.singletonList(room), pageable, 1);

            when(roomRepository.findAllByDeletedFalse(any(Pageable.class))).thenReturn(roomPage);
            when(roomMapper.toResponse(any(Room.class))).thenReturn(roomResponse);

            // When
            PageResponse<RoomResponse> result = roomService.getAllRooms(1, 10, "name", "asc", null);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getItems()).hasSize(1);
            verify(roomRepository).findAllByDeletedFalse(any(Pageable.class));
        }

        @Test
        void getRoomByCinema_Success() {
            // Given
            when(cinemaRepository.findCinemaById("cinema-001")).thenReturn(cinema);
            Pageable pageable = PageRequest.of(0, 10, Sort.by("name").ascending());
            Page<Room> roomPage = new PageImpl<>(Collections.singletonList(room), pageable, 1);

            when(roomRepository.findRoomsByCinema(eq(cinema), any(Pageable.class))).thenReturn(roomPage);
            when(roomMapper.toResponse(any(Room.class))).thenReturn(roomResponse);

            // When
            PageResponse<RoomResponse> result = roomService.getRoomByCinema("cinema-001", 1, 10, "name", "asc");

            // Then
            assertThat(result).isNotNull();
            verify(roomRepository).findRoomsByCinema(eq(cinema), any(Pageable.class));
        }

        @Test
        void getAllRooms_WithKeyword_Success() {
            // Given
            Page<Room> roomPage = new PageImpl<>(Collections.singletonList(room));
            when(roomRepository.findByNameContainingIgnoreCaseAndDeletedFalse(eq("Room 1"), any(Pageable.class))).thenReturn(roomPage);
            when(roomMapper.toResponse(any(Room.class))).thenReturn(roomResponse);

            // When
            PageResponse<RoomResponse> result = roomService.getAllRooms(1, 10, "name", "desc", "Room 1");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getItems()).hasSize(1);
            verify(roomRepository).findByNameContainingIgnoreCaseAndDeletedFalse(eq("Room 1"), any(Pageable.class));
        }

        @Test
        void getRoomByCinema_Descending_Success() {
            // Given
            when(cinemaRepository.findCinemaById("cinema-001")).thenReturn(cinema);
            Page<Room> roomPage = new PageImpl<>(Collections.singletonList(room));

            when(roomRepository.findRoomsByCinema(eq(cinema), any(Pageable.class))).thenReturn(roomPage);
            when(roomMapper.toResponse(any(Room.class))).thenReturn(roomResponse);

            // When
            PageResponse<RoomResponse> result = roomService.getRoomByCinema("cinema-001", 1, 10, "name", "desc");

            // Then
            assertThat(result).isNotNull();
            verify(roomRepository).findRoomsByCinema(eq(cinema), any(Pageable.class));
        }
    }

    @Nested
    class UpdateRoomTests {
        @Test
        void updateRoom_Success() {
            // Given
            UpdateRoomRequest updateRequest = new UpdateRoomRequest("Updated Room", RoomType.FOUR_D, 120);
            when(roomRepository.findById("room-001")).thenReturn(Optional.of(room));
            when(roomRepository.save(any(Room.class))).thenReturn(room);
            when(roomMapper.toResponse(any(Room.class))).thenReturn(roomResponse);

            // When
            roomService.updateRoom("room-001", updateRequest);

            // Then
            verify(roomMapper).updateRoom(updateRequest, room);
            verify(roomRepository).save(room);
        }

        @Test
        void updateRoom_NotFound_ThrowsException() {
            // Given
            when(roomRepository.findById("invalid")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> roomService.updateRoom("invalid", mock(UpdateRoomRequest.class)))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_NOT_FOUND);
        }
    }
}
