package org.example.cinemaBooking.unit.service;

import org.example.cinemaBooking.DTO.Response.Seat.SeatTypeResponse;
import org.example.cinemaBooking.Entity.SeatType;
import org.example.cinemaBooking.Exception.AppException;
import org.example.cinemaBooking.Exception.ErrorCode;
import org.example.cinemaBooking.Mapper.SeatTypeMapper;
import org.example.cinemaBooking.Repository.SeatTypeRepository;
import org.example.cinemaBooking.Service.Seat.SeatTypeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SeatTypeServiceTest {

    @Mock
    private SeatTypeRepository seatTypeRepository;
    @Mock
    private SeatTypeMapper seatTypeMapper;

    @InjectMocks
    private SeatTypeService seatTypeService;

    private SeatType seatType;

    @BeforeEach
    void setUp() {
        seatType = new SeatType();
        seatType.setId("type-001");
    }

    @Nested
    class GetAllTests {
        @Test
        void getAll_Success() {
            when(seatTypeRepository.findAllByDeletedAtIsNull()).thenReturn(Collections.singletonList(seatType));
            when(seatTypeMapper.toResponse(any(SeatType.class))).thenReturn(mock(SeatTypeResponse.class));

            List<SeatTypeResponse> results = seatTypeService.getAll();

            assertThat(results).hasSize(1);
            verify(seatTypeRepository).findAllByDeletedAtIsNull();
        }
    }

    @Nested
    class GetByIdTests {
        @Test
        void getById_Success() {
            when(seatTypeRepository.findById("type-001")).thenReturn(Optional.of(seatType));
            when(seatTypeMapper.toResponse(any(SeatType.class))).thenReturn(mock(SeatTypeResponse.class));

            SeatTypeResponse result = seatTypeService.getById("type-001");

            assertThat(result).isNotNull();
            verify(seatTypeRepository).findById("type-001");
        }

        @Test
        void getById_NotFound_ThrowsException() {
            when(seatTypeRepository.findById("invalid")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> seatTypeService.getById("invalid"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEAT_TYPE_NOT_FOUND);
        }
    }
}
