package org.example.cinemaBooking.Service.Showtime;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
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
import org.example.cinemaBooking.Shared.enums.SeatStatus;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class ShowTimeSeatService {

    ShowtimeSeatRepository showtimeSeatRepository;
    ShowtimeRepository showtimeRepository;
    UserRepository userRepository;
    ShowtimeSeatMapper showtimeSeatMapper;

    /**
     * Inject class riêng để gọi @Transactional qua Spring proxy.
     * Giải quyết SonarQube: "Call transactional methods via an injected dependency instead of this."
     */
    ShowtimeSeatTransactionalService seatTxService;

    static final int MAX_RETRY = 3;

    // ────────────────────────────────────────────────────────────
    // PUBLIC API
    // ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public SeatMapResponse getSeatMap(String showtimeId) {
        List<ShowtimeSeat> showtimeSeats =
                showtimeSeatRepository.findAllByShowtimeIdWithDetails(showtimeId);

        List<ShowtimeSeatResponse> responses = showtimeSeats.stream()
                .map(showtimeSeatMapper::toResponse)
                .toList();

        Map<String, List<ShowtimeSeatResponse>> seatMap = responses.stream()
                .collect(Collectors.groupingBy(
                        ShowtimeSeatResponse::seatRow,
                        TreeMap::new,
                        Collectors.toList()
                ));

        int availableSeats = (int) responses.stream()
                .filter(s -> s.status() == SeatStatus.AVAILABLE)
                .count();

        return new SeatMapResponse(showtimeId, responses.size(), availableSeats, seatMap);
    }

    @Transactional(readOnly = true)
    public List<ShowtimeSeatResponse> getMyLockedSeats(String showtimeId) {
        String userId = getCurrentUser().getId();
        return showtimeSeatRepository
                .findLockedByShowtimeAndUser(showtimeId, userId)
                .stream()
                .map(showtimeSeatMapper::toResponse)
                .toList();
    }

    /**
     * Lock ghế với retry khi Optimistic Lock conflict.
     * Gọi seatTxService.doLockSeats() qua injected bean → Spring proxy intercept đúng.
     */
    public List<ShowtimeSeatResponse> lockSeats(String showtimeId, LockSeatRequest request) {
        int attempt = 0;
        while (true) {
            try {
                return seatTxService.doLockSeats(showtimeId, request);
            } catch (OptimisticLockingFailureException ex) {
                attempt++;
                if (attempt >= MAX_RETRY) {
                    log.warn("lockSeats: conflict after {} attempts — showtimeId={}, seatIds={}",
                            attempt, showtimeId, request.seatIds());
                    throw new AppException(ErrorCode.SEAT_ALREADY_LOCKED);
                }
                log.debug("lockSeats: retry {}/{}", attempt, MAX_RETRY);
            }
        }
    }

    /**
     * Unlock ghế với retry khi Optimistic Lock conflict.
     */
    public List<ShowtimeSeatResponse> unlockSeats(String showtimeId, UnlockSeatRequest request) {
        int attempt = 0;
        while (true) {
            try {
                return seatTxService.doUnlockSeats(showtimeId, request);
            } catch (OptimisticLockingFailureException ex) {
                attempt++;
                if (attempt >= MAX_RETRY) {
                    log.warn("unlockSeats: conflict after {} attempts", attempt);
                    throw new AppException(ErrorCode.SEAT_ALREADY_LOCKED);
                }
                log.debug("unlockSeats: retry {}/{}", attempt, MAX_RETRY);
            }
        }
    }

    /**
     * Confirm booking: chuyển ghế LOCKED → BOOKED sau khi payment thành công.
     */
    @Transactional
    public void confirmBooking(String showtimeId, List<String> seatIds, String userId) {
        List<ShowtimeSeat> targets =
                showtimeSeatRepository.findByShowtimeIdAndSeatIds(showtimeId, seatIds);

        LocalDateTime now = LocalDateTime.now();

        for (ShowtimeSeat ss : targets) {
            boolean validLock = ss.getStatus() == SeatStatus.LOCKED
                    && userId.equals(ss.getLockedByUser())
                    && ss.getLockedUntil() != null
                    && ss.getLockedUntil().isAfter(now);

            if (!validLock) {
                throw new AppException(ErrorCode.SEAT_LOCK_MISMATCH);
            }
            ss.setStatus(SeatStatus.BOOKED);
            ss.setLockedUntil(null);
            ss.setLockedByUser(null);
        }

        showtimeSeatRepository.saveAll(targets);

        log.info("Confirmed booking: {} seat(s) in showtime {} for user {}",
                targets.size(), showtimeId, userId);
    }

    /**
     * Release ghế BOOKED → AVAILABLE khi refund / cancel booking đã CONFIRMED.
     */
    @Transactional
    public void releaseBookedSeats(String showtimeId, List<String> seatIds) {
        List<ShowtimeSeat> targets =
                showtimeSeatRepository.findByShowtimeIdAndSeatIds(showtimeId, seatIds);

        targets.forEach(seatTxService::releaseSeat);
        showtimeSeatRepository.saveAll(targets);
        seatTxService.syncAvailableSeats(showtimeId);

        log.info("Released {} booked seat(s) in showtime {}", targets.size(), showtimeId);
    }

    /**
     * Release ghế LOCKED → AVAILABLE khi cancel booking PENDING.
     * Không throw nếu ghế đã AVAILABLE (job expire chạy trước).
     */
    @Transactional
    public void releaseLockedSeats(String showtimeId, List<String> seatIds) {
        List<ShowtimeSeat> targets =
                showtimeSeatRepository.findByShowtimeIdAndSeatIds(showtimeId, seatIds);

        targets.forEach(ss -> {
            if (ss.getStatus() == SeatStatus.LOCKED) {
                seatTxService.releaseSeat(ss);
            }
        });

        showtimeSeatRepository.saveAll(targets);
        seatTxService.syncAvailableSeats(showtimeId);

        log.info("Released {} locked seat(s) in showtime {}", targets.size(), showtimeId);
    }

    // ────────────────────────────────────────────────────────────
    // SCHEDULED JOB
    // ────────────────────────────────────────────────────────────

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void releaseExpiredLocks() {
        LocalDateTime now = LocalDateTime.now();

        List<String> affectedShowtimeIds =
                showtimeSeatRepository.findShowtimeIdsWithExpiredLocks(now);

        if (affectedShowtimeIds.isEmpty()) return;

        int count = showtimeSeatRepository.releaseExpiredLocks(now);

        affectedShowtimeIds.forEach(seatTxService::syncAvailableSeats);

        log.info("Released {} expired seat lock(s) at {} — affected {} showtime(s)",
                count, now, affectedShowtimeIds.size());
    }

    // ────────────────────────────────────────────────────────────
    // PRIVATE
    // ────────────────────────────────────────────────────────────

    private UserEntity getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return userRepository.findUserEntityByUsername(auth.getName())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }
}