package org.example.cinemaBooking.unit.service;

import org.example.cinemaBooking.DTO.Request.Seat.BulkSeatRequest;
import org.example.cinemaBooking.DTO.Request.Seat.CreateSeatRequest;
import org.example.cinemaBooking.DTO.Request.Seat.UpdateSeatRequest;
import org.example.cinemaBooking.DTO.Response.Seat.SeatResponse;
import org.example.cinemaBooking.Entity.Room;
import org.example.cinemaBooking.Entity.Seat;
import org.example.cinemaBooking.Entity.SeatType;
import org.example.cinemaBooking.Exception.AppException;
import org.example.cinemaBooking.Exception.ErrorCode;
import org.example.cinemaBooking.Mapper.SeatMapper;
import org.example.cinemaBooking.Repository.RoomRepository;
import org.example.cinemaBooking.Repository.SeatRepository;
import org.example.cinemaBooking.Repository.SeatTypeRepository;
import org.example.cinemaBooking.Service.Seat.SeatService;
import org.example.cinemaBooking.Shared.enums.SeatTypeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SeatServiceTest {

    @Mock
    private SeatRepository seatRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private SeatTypeRepository seatTypeRepository;
    @Mock
    private SeatMapper seatMapper;

    @InjectMocks
    private SeatService seatService;

    private Room room;
    private SeatType seatType;
    private Seat seat;

    @BeforeEach
    void setUp() {
        room = new Room();
        room.setId("room-001");

        seatType = new SeatType();
        seatType.setId("type-001");
        seatType.setName(SeatTypeEnum.STANDARD);

        seat = Seat.builder()
                .seatRow("A")
                .seatNumber(1)
                .room(room)
                .seatType(seatType)
                .active(true)
                .build();
        seat.setId("seat-001");
    }

    @Nested
    class CreateSeatTests {
        @Test
        void createSeat_Success() {
            // Given
            CreateSeatRequest request = new CreateSeatRequest("A", 1, "room-001", "type-001");
            when(roomRepository.findById("room-001")).thenReturn(Optional.of(room));
            when(seatTypeRepository.findById("type-001")).thenReturn(Optional.of(seatType));
            when(seatRepository.existsByRoomIdAndSeatRowAndSeatNumberAndDeletedAtIsNull(anyString(), anyString(), anyInt())).thenReturn(false);
            when(seatRepository.save(any(Seat.class))).thenReturn(seat);
            when(seatMapper.toResponse(any(Seat.class))).thenReturn(mock(SeatResponse.class));

            // When
            seatService.createSeat(request);

            // Then
            verify(seatRepository).save(any(Seat.class));
        }

        @Test
        void createSeat_AlreadyExists_ThrowsException() {
            // Given
            CreateSeatRequest request = new CreateSeatRequest("A", 1, "room-001", "type-001");
            when(roomRepository.findById("room-001")).thenReturn(Optional.of(room));
            when(seatTypeRepository.findById("type-001")).thenReturn(Optional.of(seatType));
            when(seatRepository.existsByRoomIdAndSeatRowAndSeatNumberAndDeletedAtIsNull(anyString(), anyString(), anyInt())).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> seatService.createSeat(request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEAT_ALREADY_EXISTS);
        }

        @Test
        void createSeat_RoomNotFound_ThrowsException() {
            CreateSeatRequest request = new CreateSeatRequest("A", 1, "invalid", "type-001");
            when(roomRepository.findById("invalid")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> seatService.createSeat(request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_NOT_FOUND);
        }

        @Test
        void createSeat_TypeNotFound_ThrowsException() {
            CreateSeatRequest request = new CreateSeatRequest("A", 1, "room-001", "invalid");
            when(roomRepository.findById("room-001")).thenReturn(Optional.of(room));
            when(seatTypeRepository.findById("invalid")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> seatService.createSeat(request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEAT_TYPE_NOT_FOUND);
        }
    }

    @Nested
    class BulkSeatTests {
        @Test
        void createBulkSeats_Success() {
            // Given
            BulkSeatRequest.SeatGroup group = new BulkSeatRequest.SeatGroup(
                    Arrays.asList("A", "B"), Arrays.asList(1, 2), "type-001"
            );
            BulkSeatRequest request = new BulkSeatRequest(Collections.singletonList(group));

            when(roomRepository.findById("room-001")).thenReturn(Optional.of(room));
            when(seatTypeRepository.findById("type-001")).thenReturn(Optional.of(seatType));
            when(seatRepository.existsByRoomIdAndSeatRowAndSeatNumberAndDeletedAtIsNull(any(), any(), any())).thenReturn(false);
            // 2 rows * 2 numbers = 4 seats
            when(seatRepository.saveAll(anyList())).thenReturn(Arrays.asList(new Seat(), new Seat(), new Seat(), new Seat()));

            // When
            List<SeatResponse> results = seatService.createBulkSeats("room-001", request);

            // Then
            assertThat(results).hasSize(4);
            verify(seatRepository).saveAll(argThat(it -> {
                int count = 0;
                for (Object ignored : it) count++;
                return count == 4;
            }));
        }

        @Test
        void createBulkSeats_EmptyGroup_IsSkipped() {
            // Given
            BulkSeatRequest.SeatGroup emptyGroup = new BulkSeatRequest.SeatGroup(
                    Collections.emptyList(), Collections.emptyList(), "type-001"
            );
            BulkSeatRequest request = new BulkSeatRequest(Collections.singletonList(emptyGroup));
            when(roomRepository.findById("room-001")).thenReturn(Optional.of(room));

            // When
            List<SeatResponse> results = seatService.createBulkSeats("room-001", request);

            // Then
            assertThat(results).isEmpty();
            verify(seatRepository, never()).saveAll(anyList());
        }

        @Test
        void createBulkSeats_AlreadyExists_IsSkipped() {
            BulkSeatRequest.SeatGroup group = new BulkSeatRequest.SeatGroup(
                    Collections.singletonList("A"), Collections.singletonList(1), "type-001"
            );
            BulkSeatRequest request = new BulkSeatRequest(Collections.singletonList(group));

            when(roomRepository.findById("room-001")).thenReturn(Optional.of(room));
            when(seatTypeRepository.findById("type-001")).thenReturn(Optional.of(seatType));
            when(seatRepository.existsByRoomIdAndSeatRowAndSeatNumberAndDeletedAtIsNull(anyString(), eq("A"), eq(1))).thenReturn(true);

            List<SeatResponse> results = seatService.createBulkSeats("room-001", request);
            assertThat(results).isEmpty();
        }

        @Test
        void createBulkSeats_TypeNotFound_ThrowsException() {
            BulkSeatRequest.SeatGroup group = new BulkSeatRequest.SeatGroup(
                    Collections.singletonList("A"), Collections.singletonList(1), "invalid"
            );
            BulkSeatRequest request = new BulkSeatRequest(Collections.singletonList(group));

            when(roomRepository.findById("room-001")).thenReturn(Optional.of(room));
            when(seatTypeRepository.findById("invalid")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> seatService.createBulkSeats("room-001", request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEAT_TYPE_NOT_FOUND);
        }
    }

    @Nested
    class UpdateDeleteTests {
        @Test
        void updateSeat_Success() {
            // Given
            UpdateSeatRequest request = new UpdateSeatRequest("B", 2, true, "type-001", true);
            when(seatRepository.findById("seat-001")).thenReturn(Optional.of(seat));
            when(seatTypeRepository.findById("type-001")).thenReturn(Optional.of(seatType));
            when(seatRepository.save(any())).thenReturn(seat);
            when(seatMapper.toResponse(any())).thenReturn(mock(SeatResponse.class));

            // When
            seatService.updateSeat("seat-001", request);

            // Then
            verify(seatMapper).updateSeat(seat, request);
            verify(seatRepository).save(seat);
        }

        @Test
        void deleteSeat_Success() {
            // Given
            when(seatRepository.findById("seat-001")).thenReturn(Optional.of(seat));

            // When
            seatService.deleteSeat("seat-001");

            // Then
            assertThat(seat.getDeletedAt()).isNotNull();
            verify(seatRepository).save(seat);
        }

        @Test
        void updateSeat_NotFound_ThrowsException() {
            when(seatRepository.findById("invalid")).thenReturn(Optional.empty());
            UpdateSeatRequest request = new UpdateSeatRequest("B", 2, true, "type-001", true);
            assertThatThrownBy(() -> seatService.updateSeat("invalid", request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEAT_NOT_FOUND);
        }

        @Test
        void deleteSeat_NotFound_ThrowsException() {
            when(seatRepository.findById("invalid")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> seatService.deleteSeat("invalid"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEAT_NOT_FOUND);
        }
    }

    @Nested
    class ListTests {
        @Test
        void getSeatsByRoom_Success() {
            // Given
            when(seatRepository.findAllByRoomIdFetch("room-001")).thenReturn(Collections.singletonList(seat));
            when(seatMapper.toResponse(any())).thenReturn(mock(SeatResponse.class));

            // When
            List<SeatResponse> results = seatService.getSeatsByRoom("room-001");

            // Then
            assertThat(results).hasSize(1);
            verify(seatRepository).findAllByRoomIdFetch("room-001");
        }

        @Test
        void getSeat_Success() {
            when(seatRepository.findById("seat-001")).thenReturn(Optional.of(seat));
            when(seatMapper.toResponse(seat)).thenReturn(mock(SeatResponse.class));

            SeatResponse result = seatService.getSeat("seat-001");
            assertThat(result).isNotNull();
        }

        @Test
        void getSeat_NotFound_ThrowsException() {
            when(seatRepository.findById("invalid")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> seatService.getSeat("invalid"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEAT_NOT_FOUND);
        }
    }
}
