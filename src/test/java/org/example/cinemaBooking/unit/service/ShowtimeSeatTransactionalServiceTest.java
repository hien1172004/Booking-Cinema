package org.example.cinemaBooking.unit.service;

import org.example.cinemaBooking.DTO.Request.Seat.LockSeatRequest;
import org.example.cinemaBooking.DTO.Request.Seat.UnlockSeatRequest;
import org.example.cinemaBooking.DTO.Response.Showtime.ShowtimeSeatResponse;
import org.example.cinemaBooking.Entity.Showtime;
import org.example.cinemaBooking.Entity.ShowtimeSeat;
import org.example.cinemaBooking.Entity.UserEntity;
import org.example.cinemaBooking.Exception.AppException;
import org.example.cinemaBooking.Exception.ErrorCode;
import org.example.cinemaBooking.Mapper.ShowtimeSeatMapper;
import org.example.cinemaBooking.Repository.ShowtimeRepository;
import org.example.cinemaBooking.Repository.ShowtimeSeatRepository;
import org.example.cinemaBooking.Repository.UserRepository;
import org.example.cinemaBooking.Service.Showtime.ShowtimeSeatTransactionalService;
import org.example.cinemaBooking.Shared.enums.SeatStatus;
import org.example.cinemaBooking.Shared.enums.ShowTimeStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
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
public class ShowtimeSeatTransactionalServiceTest {

    @Mock private ShowtimeSeatRepository showtimeSeatRepository;
    @Mock private ShowtimeRepository showtimeRepository;
    @Mock private UserRepository userRepository;
    @Mock private ShowtimeSeatMapper showtimeSeatMapper;
    @Mock private Authentication authentication;
    @Mock private SecurityContext securityContext;

    @InjectMocks
    private ShowtimeSeatTransactionalService service;

    private UserEntity user;
    private Showtime showtime;
    private ShowtimeSeat showtimeSeat;
    private MockedStatic<SecurityContextHolder> mockedSecurityContextHolder;

    @BeforeEach
    void setUp() {
        user = new UserEntity();
        user.setId("u-001");
        user.setUsername("testuser");

        showtime = new Showtime();
        showtime.setId("st-001");
        showtime.setStatus(ShowTimeStatus.SCHEDULED);
        showtime.setStartTime(java.time.LocalDateTime.now().plusHours(1));

        showtimeSeat = new ShowtimeSeat();
        showtimeSeat.setId("ss-001");
        showtimeSeat.setStatus(SeatStatus.AVAILABLE);

        mockAuthentication();
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
    class LockSeatsTests {

        @Test
        void doLockSeats_Available_Success() {
            LockSeatRequest request = new LockSeatRequest(Collections.singletonList("ss-001"));
            when(showtimeRepository.findById("st-001")).thenReturn(Optional.of(showtime));
            when(showtimeSeatRepository.findByShowtimeIdAndSeatIds("st-001", request.seatIds()))
                    .thenReturn(Collections.singletonList(showtimeSeat));
            when(showtimeSeatMapper.toResponse(any())).thenReturn(mock(ShowtimeSeatResponse.class));

            List<ShowtimeSeatResponse> res = service.doLockSeats("st-001", request);

            assertThat(res).hasSize(1);
            assertThat(showtimeSeat.getStatus()).isEqualTo(SeatStatus.LOCKED);
            assertThat(showtimeSeat.getLockedByUser()).isEqualTo("u-001");
            verify(showtimeSeatRepository).saveAll(any());
            verify(showtimeRepository).syncAvailableSeats("st-001");
        }

        @Test
        void doLockSeats_ShowtimeInvalid_ThrowsException() {
            showtime.setStatus(ShowTimeStatus.FINISHED);
            LockSeatRequest request = new LockSeatRequest(Collections.singletonList("ss-001"));
            when(showtimeRepository.findById("st-001")).thenReturn(Optional.of(showtime));

            assertThatThrownBy(() -> service.doLockSeats("st-001", request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SHOWTIME_STATE_INVALID);
        }

        @Test
        void doLockSeats_SeatNotFound_ThrowsException() {
            LockSeatRequest request = new LockSeatRequest(Collections.singletonList("ss-001"));
            when(showtimeRepository.findById("st-001")).thenReturn(Optional.of(showtime));
            when(showtimeSeatRepository.findByShowtimeIdAndSeatIds("st-001", request.seatIds()))
                    .thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> service.doLockSeats("st-001", request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEAT_NOT_FOUND);
        }

        @Test
        void doLockSeats_AlreadyLockedBySameUser_ExtendSuccess() {
            showtimeSeat.setStatus(SeatStatus.LOCKED);
            showtimeSeat.setLockedByUser("u-001");
            showtimeSeat.setLockedUntil(LocalDateTime.now().plusMinutes(5));

            LockSeatRequest request = new LockSeatRequest(Collections.singletonList("ss-001"));
            when(showtimeRepository.findById("st-001")).thenReturn(Optional.of(showtime));
            when(showtimeSeatRepository.findByShowtimeIdAndSeatIds("st-001", request.seatIds()))
                    .thenReturn(Collections.singletonList(showtimeSeat));

            service.doLockSeats("st-001", request);

            assertThat(showtimeSeat.getStatus()).isEqualTo(SeatStatus.LOCKED);
            verify(showtimeSeatRepository).saveAll(any());
        }

        @Test
        void doLockSeats_AlreadyLockedExpired_LockSuccess() {
            showtimeSeat.setStatus(SeatStatus.LOCKED);
            showtimeSeat.setLockedByUser("other");
            showtimeSeat.setLockedUntil(LocalDateTime.now().minusMinutes(1)); // expired

            LockSeatRequest request = new LockSeatRequest(Collections.singletonList("ss-001"));
            when(showtimeRepository.findById("st-001")).thenReturn(Optional.of(showtime));
            when(showtimeSeatRepository.findByShowtimeIdAndSeatIds("st-001", request.seatIds()))
                    .thenReturn(Collections.singletonList(showtimeSeat));

            service.doLockSeats("st-001", request);

            assertThat(showtimeSeat.getStatus()).isEqualTo(SeatStatus.LOCKED);
            assertThat(showtimeSeat.getLockedByUser()).isEqualTo("u-001");
            verify(showtimeSeatRepository).saveAll(any());
        }

        @Test
        void doLockSeats_AlreadyLockedByOther_ThrowsException() {
            showtimeSeat.setStatus(SeatStatus.LOCKED);
            showtimeSeat.setLockedByUser("other");
            showtimeSeat.setLockedUntil(LocalDateTime.now().plusMinutes(5)); // not expired

            LockSeatRequest request = new LockSeatRequest(Collections.singletonList("ss-001"));
            when(showtimeRepository.findById("st-001")).thenReturn(Optional.of(showtime));
            when(showtimeSeatRepository.findByShowtimeIdAndSeatIds("st-001", request.seatIds()))
                    .thenReturn(Collections.singletonList(showtimeSeat));

            assertThatThrownBy(() -> service.doLockSeats("st-001", request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEAT_ALREADY_LOCKED);
        }

        @Test
        void doLockSeats_AlreadyBooked_ThrowsException() {
            showtimeSeat.setStatus(SeatStatus.BOOKED);
            LockSeatRequest request = new LockSeatRequest(Collections.singletonList("ss-001"));
            when(showtimeRepository.findById("st-001")).thenReturn(Optional.of(showtime));
            when(showtimeSeatRepository.findByShowtimeIdAndSeatIds("st-001", request.seatIds()))
                    .thenReturn(Collections.singletonList(showtimeSeat));

            assertThatThrownBy(() -> service.doLockSeats("st-001", request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEAT_ALREADY_BOOKED);
        }
    }

    @Nested
    class UnlockSeatsTests {

        @Test
        void doUnlockSeats_Success() {
            showtimeSeat.setStatus(SeatStatus.LOCKED);
            showtimeSeat.setLockedByUser("u-001");

            UnlockSeatRequest request = new UnlockSeatRequest(Collections.singletonList("ss-001"));
            when(showtimeSeatRepository.findByShowtimeIdAndSeatIds("st-001", request.seatIds()))
                    .thenReturn(Collections.singletonList(showtimeSeat));
            when(showtimeSeatMapper.toResponse(any())).thenReturn(mock(ShowtimeSeatResponse.class));

            List<ShowtimeSeatResponse> res = service.doUnlockSeats("st-001", request);

            assertThat(res).hasSize(1);
            assertThat(showtimeSeat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
            assertThat(showtimeSeat.getLockedByUser()).isNull();
            verify(showtimeSeatRepository).saveAll(any());
            verify(showtimeRepository).syncAvailableSeats("st-001");
        }

        @Test
        void doUnlockSeats_NotLocked_ThrowsException() {
            showtimeSeat.setStatus(SeatStatus.AVAILABLE);
            UnlockSeatRequest request = new UnlockSeatRequest(Collections.singletonList("ss-001"));
            when(showtimeSeatRepository.findByShowtimeIdAndSeatIds("st-001", request.seatIds()))
                    .thenReturn(Collections.singletonList(showtimeSeat));

            assertThatThrownBy(() -> service.doUnlockSeats("st-001", request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEAT_NOT_LOCKED);
        }

        @Test
        void doUnlockSeats_Forbidden_ThrowsException() {
            showtimeSeat.setStatus(SeatStatus.LOCKED);
            showtimeSeat.setLockedByUser("other");

            UnlockSeatRequest request = new UnlockSeatRequest(Collections.singletonList("ss-001"));
            when(showtimeSeatRepository.findByShowtimeIdAndSeatIds("st-001", request.seatIds()))
                    .thenReturn(Collections.singletonList(showtimeSeat));

            assertThatThrownBy(() -> service.doUnlockSeats("st-001", request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEAT_LOCK_FORBIDDEN);
        }
        
        @Test
        void doUnlockSeats_SeatNotFound_ThrowsException() {
            UnlockSeatRequest request = new UnlockSeatRequest(Collections.singletonList("ss-001"));
            when(showtimeSeatRepository.findByShowtimeIdAndSeatIds("st-001", request.seatIds()))
                    .thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> service.doUnlockSeats("st-001", request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEAT_NOT_FOUND);
        }
    }
}
