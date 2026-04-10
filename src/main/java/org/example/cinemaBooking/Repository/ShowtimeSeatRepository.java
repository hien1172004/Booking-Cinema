// ShowtimeSeatRepository.java
package org.example.cinemaBooking.Repository;

import org.example.cinemaBooking.Entity.ShowtimeSeat;
import org.example.cinemaBooking.Shared.enums.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShowtimeSeatRepository extends JpaRepository<ShowtimeSeat, String> {

    /**
     * Lấy toàn bộ ghế của 1 suất chiếu, kèm Seat + SeatType (tránh N+1)
     */
    @Query("""
            SELECT ss FROM ShowtimeSeat ss
            JOIN FETCH ss.seat s
            JOIN FETCH s.seatType
            WHERE ss.showtime.id = :showtimeId
              AND ss.deletedAt IS NULL
            ORDER BY s.seatRow ASC, s.seatNumber ASC
            """)
    List<ShowtimeSeat> findAllByShowtimeIdWithDetails(@Param("showtimeId") String showtimeId);

    /**
     * Lấy 1 ghế cụ thể trong suất chiếu — dùng khi lock/unlock
     */
    @Query("""
            SELECT ss FROM ShowtimeSeat ss
            JOIN FETCH ss.seat s
            JOIN FETCH s.seatType
            JOIN FETCH ss.showtime st
            WHERE ss.showtime.id = :showtimeId
              AND ss.seat.id    = :seatId
              AND ss.deletedAt IS NULL
            """)
    Optional<ShowtimeSeat> findByShowtimeIdAndSeatId(
            @Param("showtimeId") String showtimeId,
            @Param("seatId")     String seatId);

    /**
     * Lấy nhiều ghế trong 1 suất chiếu — dùng khi lock batch
     */
    @Query("""
            SELECT ss FROM ShowtimeSeat ss
            JOIN FETCH ss.seat s
            JOIN FETCH s.seatType
            WHERE ss.showtime.id = :showtimeId
              AND ss.seat.id IN :seatIds
              AND ss.deletedAt IS NULL
            ORDER BY s.seatRow, s.seatNumber
            """)

    List<ShowtimeSeat> findByShowtimeIdAndSeatIds(
            @Param("showtimeId") String showtimeId,
            @Param("seatIds")    List<String> seatIds);

    /**
     * Release tất cả ghế LOCKED đã hết hạn — scheduled job gọi
     */
    @Modifying
    @Query("""
            UPDATE ShowtimeSeat ss
            SET ss.status       = org.example.cinemaBooking.Shared.enums.SeatStatus.AVAILABLE,
                ss.lockedUntil  = NULL,
                ss.lockedByUser = NULL
            WHERE ss.status      = org.example.cinemaBooking.Shared.enums.SeatStatus.LOCKED
              AND ss.lockedUntil < :now
              AND ss.deletedAt IS NULL
            """)
    int releaseExpiredLocks(@Param("now") LocalDateTime now);

    /**
     * Đếm ghế còn trống — dùng để sync lại cache availableSeats trên Showtime
     */
    @Query("""
            SELECT COUNT(ss) FROM ShowtimeSeat ss
            WHERE ss.showtime.id = :showtimeId
              AND ss.status      = :status
              AND ss.deletedAt IS NULL
            """)
    int countByShowtimeIdAndStatus(
            @Param("showtimeId") String showtimeId,
            @Param("status")     SeatStatus status);

    /**
     * Kiểm tra user đang giữ ghế nào trong suất chiếu
     */
    @Query("""
            SELECT ss FROM ShowtimeSeat ss
            JOIN FETCH ss.seat s
            JOIN FETCH s.seatType
            WHERE ss.showtime.id  = :showtimeId
              AND ss.lockedByUser = :userId
              AND ss.status       = org.example.cinemaBooking.Shared.enums.SeatStatus.LOCKED
              AND ss.deletedAt IS NULL
            """)
    List<ShowtimeSeat> findLockedByShowtimeAndUser(
            @Param("showtimeId") String showtimeId,
            @Param("userId")     String userId);

    /**
     * Lấy danh sách showtimeId có ghế lock hết hạn — dùng trước bulk UPDATE
     * để biết showtime nào cần sync lại cache availableSeats.
     */
    @Query("""
            SELECT DISTINCT ss.showtime.id FROM ShowtimeSeat ss
            WHERE ss.status      = org.example.cinemaBooking.Shared.enums.SeatStatus.LOCKED
              AND ss.lockedUntil < :now
              AND ss.deletedAt IS NULL
            """)
    List<String> findShowtimeIdsWithExpiredLocks(@Param("now") LocalDateTime now);
}