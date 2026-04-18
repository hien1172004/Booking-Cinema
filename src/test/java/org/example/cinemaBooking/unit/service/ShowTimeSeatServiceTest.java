package org.example.cinemaBooking.unit.service;

import org.example.cinemaBooking.DTO.Request.Seat.LockSeatRequest;
import org.example.cinemaBooking.DTO.Request.Seat.UnlockSeatRequest;
import org.example.cinemaBooking.DTO.Response.Showtime.SeatMapResponse;
import org.example.cinemaBooking.DTO.Response.Showtime.ShowtimeSeatResponse;
import org.example.cinemaBooking.Entity.ShowtimeSeat;
import org.example.cinemaBooking.Entity.UserEntity;
import org.example.cinemaBooking.Exception.AppException;
import org.example.cinemaBooking.Exception.ErrorCode;
import org.example.cinemaBooking.Mapper.ShowtimeSeatMapper;
import org.example.cinemaBooking.Repository.ShowtimeRepository;
import org.example.cinemaBooking.Repository.ShowtimeSeatRepository;
import org.example.cinemaBooking.Repository.UserRepository;
import org.example.cinemaBooking.Service.Showtime.ShowTimeSeatService;
import org.example.cinemaBooking.Service.Showtime.ShowtimeSeatTransactionalService;
import org.example.cinemaBooking.Shared.enums.SeatStatus;
import org.example.cinemaBooking.Shared.enums.SeatTypeEnum;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ShowTimeSeatServiceTest {

    @Mock private ShowtimeSeatRepository showtimeSeatRepository;
    @Mock private ShowtimeRepository showtimeRepository;
    @Mock private UserRepository userRepository;
    @Mock private ShowtimeSeatMapper showtimeSeatMapper;
    @Mock private ShowtimeSeatTransactionalService seatTxService;

    @InjectMocks
    private ShowTimeSeatService showTimeSeatService;

    @Mock private Authentication authentication;
    @Mock private SecurityContext securityContext;

    private UserEntity user;
    private MockedStatic<SecurityContextHolder> mockedSecurityContextHolder;
    private ShowtimeSeat showtimeSeat;

    @BeforeEach
    void setUp() {
        user = new UserEntity();
        user.setId("u-001");
        user.setUsername("testuser");

        showtimeSeat = new ShowtimeSeat();
        showtimeSeat.setId("ss-001");
        showtimeSeat.setStatus(SeatStatus.AVAILABLE);
    }

    private void mockAuthentication() {
        mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class);
        mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");
        when(userRepository.findUserEntityByUsername("testuser")).thenReturn(Optional.of(user));
    }

    @AfterEach
    void tearDown() {
        if (mockedSecurityContextHolder != null) {
            mockedSecurityContextHolder.close();
        }
    }

    @Nested
    class GetTests {
        @Test
        void getSeatMap_Success() {
            when(showtimeSeatRepository.findAllByShowtimeIdWithDetails("st-001")).thenReturn(Collections.singletonList(showtimeSeat));
            
            ShowtimeSeatResponse response = new ShowtimeSeatResponse(
                "ss-001", // showtimeSeatId
                "A",      // seatId
                "A",      // seatRow (should match the key expected in seatMap)
                1,         // seatNumber
                SeatTypeEnum.VIP, // seatType
                null,      // price (BigDecimal)
                SeatStatus.AVAILABLE, // status (SeatStatus)
                null,      // lockedUntil (LocalDateTime)
                null       // lockedByUser (String)
            );
            when(showtimeSeatMapper.toResponse(any())).thenReturn(response);

            SeatMapResponse res = showTimeSeatService.getSeatMap("st-001");

            assertThat(res).isNotNull();
            assertThat(res.availableSeats()).isEqualTo(1);
            assertThat(res.totalSeats()).isEqualTo(1);
            assertThat(res.seatMap()).containsKey("A");
        }

        @Test
        void getMyLockedSeats_Success() {
            mockAuthentication();
            when(showtimeSeatRepository.findLockedByShowtimeAndUser("st-001", "u-001"))
                    .thenReturn(Collections.singletonList(showtimeSeat));
            when(showtimeSeatMapper.toResponse(any())).thenReturn(mock(ShowtimeSeatResponse.class));

            List<ShowtimeSeatResponse> res = showTimeSeatService.getMyLockedSeats("st-001");

            assertThat(res).hasSize(1);
        }
    }

    @Nested
    class LockUnlockTests {
        @Test
        void lockSeats_Success() {
            LockSeatRequest req = new LockSeatRequest(Collections.singletonList("ss-001"));
            when(seatTxService.doLockSeats("st-001", req)).thenReturn(Collections.singletonList(mock(ShowtimeSeatResponse.class)));

            List<ShowtimeSeatResponse> res = showTimeSeatService.lockSeats("st-001", req);

            assertThat(res).hasSize(1);
            verify(seatTxService, times(1)).doLockSeats("st-001", req);
        }

        @Test
        void lockSeats_RetryExceeded_ThrowsException() {
            LockSeatRequest req = new LockSeatRequest(Collections.singletonList("ss-001"));
            when(seatTxService.doLockSeats("st-001", req)).thenThrow(new OptimisticLockingFailureException("conflict"));

            assertThatThrownBy(() -> showTimeSeatService.lockSeats("st-001", req))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEAT_ALREADY_LOCKED);

            verify(seatTxService, times(3)).doLockSeats("st-001", req);
        }

        @Test
        void unlockSeats_Success() {
            UnlockSeatRequest req = new UnlockSeatRequest(Collections.singletonList("ss-001"));
            when(seatTxService.doUnlockSeats("st-001", req)).thenReturn(Collections.singletonList(mock(ShowtimeSeatResponse.class)));

            List<ShowtimeSeatResponse> res = showTimeSeatService.unlockSeats("st-001", req);

            assertThat(res).hasSize(1);
            verify(seatTxService, times(1)).doUnlockSeats("st-001", req);
        }

        @Test
        void unlockSeats_RetryExceeded_ThrowsException() {
            UnlockSeatRequest req = new UnlockSeatRequest(Collections.singletonList("ss-001"));
            when(seatTxService.doUnlockSeats("st-001", req)).thenThrow(new OptimisticLockingFailureException("conflict"));

            assertThatThrownBy(() -> showTimeSeatService.unlockSeats("st-001", req))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEAT_ALREADY_LOCKED);

            verify(seatTxService, times(3)).doUnlockSeats("st-001", req);
        }
    }

    @Nested
    class VerifyAndReleaseTests {
        @Test
        void confirmBooking_Success() {
            showtimeSeat.setStatus(SeatStatus.LOCKED);
            showtimeSeat.setLockedByUser("u-001");
            showtimeSeat.setLockedUntil(LocalDateTime.now().plusMinutes(5)); // valid

            when(showtimeSeatRepository.findByShowtimeIdAndSeatIds("st-001", Collections.singletonList("ss-001")))
                    .thenReturn(Collections.singletonList(showtimeSeat));

            showTimeSeatService.confirmBooking("st-001", Collections.singletonList("ss-001"), "u-001");

            assertThat(showtimeSeat.getStatus()).isEqualTo(SeatStatus.BOOKED);
            assertThat(showtimeSeat.getLockedByUser()).isNull();
            verify(showtimeSeatRepository).saveAll(any());
        }

        @Test
        void confirmBooking_LockMismatch_ThrowsException() {
            showtimeSeat.setStatus(SeatStatus.LOCKED);
            showtimeSeat.setLockedByUser("other"); // mismatch user
            showtimeSeat.setLockedUntil(LocalDateTime.now().plusMinutes(5)); 

            when(showtimeSeatRepository.findByShowtimeIdAndSeatIds("st-001", Collections.singletonList("ss-001")))
                    .thenReturn(Collections.singletonList(showtimeSeat));

            assertThatThrownBy(() -> showTimeSeatService.confirmBooking("st-001", Collections.singletonList("ss-001"), "u-001"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEAT_LOCK_MISMATCH);
        }

        @Test
        void confirmBooking_LockExpired_ThrowsException() {
            showtimeSeat.setStatus(SeatStatus.LOCKED);
            showtimeSeat.setLockedByUser("u-001");
            showtimeSeat.setLockedUntil(LocalDateTime.now().minusMinutes(5)); // expired

            when(showtimeSeatRepository.findByShowtimeIdAndSeatIds("st-001", Collections.singletonList("ss-001")))
                    .thenReturn(Collections.singletonList(showtimeSeat));

            assertThatThrownBy(() -> showTimeSeatService.confirmBooking("st-001", Collections.singletonList("ss-001"), "u-001"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEAT_LOCK_MISMATCH);
        }

        @Test
        void releaseBookedSeats_Success() {
            when(showtimeSeatRepository.findByShowtimeIdAndSeatIds("st-001", Collections.singletonList("ss-001")))
                    .thenReturn(Collections.singletonList(showtimeSeat));

            showTimeSeatService.releaseBookedSeats("st-001", Collections.singletonList("ss-001"));

            verify(seatTxService).releaseSeat(showtimeSeat);
            verify(showtimeSeatRepository).saveAll(any());
            verify(seatTxService).syncAvailableSeats("st-001");
        }

        @Test
        void releaseLockedSeats_Success() {
            showtimeSeat.setStatus(SeatStatus.LOCKED);
            when(showtimeSeatRepository.findByShowtimeIdAndSeatIds("st-001", Collections.singletonList("ss-001")))
                    .thenReturn(Collections.singletonList(showtimeSeat));

            showTimeSeatService.releaseLockedSeats("st-001", Collections.singletonList("ss-001"));

            verify(seatTxService).releaseSeat(showtimeSeat);
            verify(showtimeSeatRepository).saveAll(any());
            verify(seatTxService).syncAvailableSeats("st-001");
        }

        @Test
        void releaseExpiredLocks_Success() {
            when(showtimeSeatRepository.findShowtimeIdsWithExpiredLocks(any())).thenReturn(Collections.singletonList("st-001"));
            when(showtimeSeatRepository.releaseExpiredLocks(any())).thenReturn(1);

            showTimeSeatService.releaseExpiredLocks();

            verify(seatTxService).syncAvailableSeats("st-001");
        }

        @Test
        void releaseExpiredLocks_Empty_DoesNothing() {
            when(showtimeSeatRepository.findShowtimeIdsWithExpiredLocks(any())).thenReturn(Collections.emptyList());

            showTimeSeatService.releaseExpiredLocks();

            verify(showtimeSeatRepository, never()).releaseExpiredLocks(any());
        }
    }
}
