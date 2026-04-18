package org.example.cinemaBooking.Service.Showtime;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
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
import org.example.cinemaBooking.Shared.enums.SeatStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class ShowtimeSeatTransactionalService {

    ShowtimeSeatRepository showtimeSeatRepository;
    ShowtimeRepository showtimeRepository;
    UserRepository userRepository;
    ShowtimeSeatMapper showtimeSeatMapper;

    static final int LOCK_DURATION_MINUTES = 10;

    // ────────────────────────────────────────────────────────────
    // LOCK
    // ────────────────────────────────────────────────────────────

    /**
     * Logic lock thực sự — 1 transaction.
     * Nếu @Version conflict → OptimisticLockingFailureException → caller
     * (ShowTimeSeatService) retry.
     */
    @Transactional
    public List<ShowtimeSeatResponse> doLockSeats(String showtimeId, LockSeatRequest request) {
        String userId = getCurrentUserId();
        Showtime showtime = getShowtimeOrThrow(showtimeId);

        if (!showtime.isBookable()) {
            throw new AppException(ErrorCode.SHOWTIME_STATE_INVALID);
        }

        List<ShowtimeSeat> targets = showtimeSeatRepository.findByShowtimeIdAndSeatIds(showtimeId, request.seatIds());

        if (targets.size() != request.seatIds().size()) {
            throw new AppException(ErrorCode.SEAT_NOT_FOUND);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lockExpiry = now.plusMinutes(LOCK_DURATION_MINUTES);

        for (ShowtimeSeat ss : targets) {
            switch (ss.getStatus()) {

                case AVAILABLE -> {
                    ss.setStatus(SeatStatus.LOCKED);
                    ss.setLockedByUser(userId);
                    ss.setLockedUntil(lockExpiry);
                }

                case LOCKED -> {
                    boolean expired = ss.getLockedUntil() != null
                            && ss.getLockedUntil().isBefore(now);

                    if (expired) {
                        releaseSeat(ss);
                        ss.setStatus(SeatStatus.LOCKED);
                        ss.setLockedByUser(userId);
                        ss.setLockedUntil(lockExpiry);
                        continue;
                    }

                    if (!userId.equals(ss.getLockedByUser())) {
                        throw new AppException(ErrorCode.SEAT_ALREADY_LOCKED);
                    }

                    ss.setLockedUntil(lockExpiry); // extend
                }

                case BOOKED -> throw new AppException(ErrorCode.SEAT_ALREADY_BOOKED);
            }
        }

        showtimeSeatRepository.saveAll(targets);
        syncAvailableSeats(showtimeId);

        log.info("User {} locked {} seat(s) in showtime {} until {}",
                userId, targets.size(), showtimeId, lockExpiry);

        return targets.stream().map(showtimeSeatMapper::toResponse).toList();
    }

    // ────────────────────────────────────────────────────────────
    // UNLOCK
    // ────────────────────────────────────────────────────────────

    /**
     * Logic unlock thực sự — 1 transaction.
     */
    @Transactional
    public List<ShowtimeSeatResponse> doUnlockSeats(String showtimeId, UnlockSeatRequest request) {
        String userId = getCurrentUserId();

        List<ShowtimeSeat> targets = showtimeSeatRepository.findByShowtimeIdAndSeatIds(showtimeId, request.seatIds());

        if (targets.size() != request.seatIds().size()) {
            throw new AppException(ErrorCode.SEAT_NOT_FOUND);
        }

        for (ShowtimeSeat ss : targets) {
            if (ss.getStatus() != SeatStatus.LOCKED) {
                throw new AppException(ErrorCode.SEAT_NOT_LOCKED);
            }
            if (!userId.equals(ss.getLockedByUser())) {
                throw new AppException(ErrorCode.SEAT_LOCK_FORBIDDEN);
            }
            releaseSeat(ss);
        }

        showtimeSeatRepository.saveAll(targets);
        syncAvailableSeats(showtimeId);

        log.info("User {} unlocked {} seat(s) in showtime {}",
                userId, targets.size(), showtimeId);

        return targets.stream().map(showtimeSeatMapper::toResponse).toList();
    }

    // ────────────────────────────────────────────────────────────
    // HELPERS
    // ────────────────────────────────────────────────────────────

    public void releaseSeat(ShowtimeSeat ss) {
        ss.setStatus(SeatStatus.AVAILABLE);
        ss.setLockedUntil(null);
        ss.setLockedByUser(null);
    }

    public void syncAvailableSeats(String showtimeId) {
        showtimeRepository.syncAvailableSeats(showtimeId);
    }

    private Showtime getShowtimeOrThrow(String showtimeId) {
        return showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOWTIME_NOT_FOUND));
    }

    private String getCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        UserEntity user = userRepository.findUserEntityByUsername(auth.getName())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return user.getId();
    }
}