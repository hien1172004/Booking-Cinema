package org.example.cinemaBooking.Repository;

import org.example.cinemaBooking.Entity.Showtime;
import org.example.cinemaBooking.Shared.enums.PaymentStatus;
import org.example.cinemaBooking.Shared.enums.ShowTimeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ShowtimeRepository
        extends JpaRepository<Showtime, String>, JpaSpecificationExecutor<Showtime> {

    // ── Fetch với JOIN để tránh N+1 ──────────────────────────────────

    @Query("""
            SELECT s FROM Showtime s
              JOIN FETCH s.movie m
              LEFT JOIN FETCH m.categories c
              JOIN FETCH s.room  r
              JOIN FETCH r.cinema cin
            WHERE s.id = :id
              AND s.deletedAt IS NULL
            """)
    Optional<Showtime> findByIdWithDetails(@Param("id") String id);

    // ── Lịch chiếu theo phim + ngày ──────────────────────────────────

    @Query("""
            SELECT s FROM Showtime s
              JOIN FETCH s.movie m
              JOIN FETCH s.room  r
              JOIN FETCH r.cinema c
            WHERE m.id         = :movieId
              AND s.startTime >= :from
              AND s.startTime <  :to
              AND s.status    != :excluded
              AND s.deletedAt IS NULL
            ORDER BY s.startTime ASC
            """)
    List<Showtime> findByMovieAndDateRange(
            @Param("movieId")  String movieId,
            @Param("from")     LocalDateTime from,
            @Param("to")       LocalDateTime to,
            @Param("excluded") ShowTimeStatus excluded
    );

    // ── Lịch chiếu theo rạp + ngày ───────────────────────────────────

    @Query("""
            SELECT s FROM Showtime s
              JOIN FETCH s.movie m
              JOIN FETCH s.room  r
              JOIN FETCH r.cinema c
            WHERE c.id         = :cinemaId
              AND s.startTime >= :from
              AND s.startTime <  :to
              AND s.status    != :excluded
              AND s.deletedAt IS NULL
            ORDER BY s.startTime ASC
            """)
    List<Showtime> findByCinemaAndDateRange(
            @Param("cinemaId") String cinemaId,
            @Param("from")     LocalDateTime from,
            @Param("to")       LocalDateTime to,
            @Param("excluded") ShowTimeStatus excluded
    );

    // ── Lịch chiếu theo tên rạp (LIKE) + ngày — dùng cho chatbot ────

    @Query("""
            SELECT s FROM Showtime s
              JOIN FETCH s.movie m
              JOIN FETCH s.room  r
              JOIN FETCH r.cinema c
            WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :cinemaName, '%'))
              AND s.startTime >= :from
              AND s.startTime <  :to
              AND s.status    != :excluded
              AND s.deletedAt IS NULL
            ORDER BY s.startTime ASC
            """)
    List<Showtime> findByCinemaNameAndDateRange(
            @Param("cinemaName") String cinemaName,
            @Param("from")       LocalDateTime from,
            @Param("to")         LocalDateTime to,
            @Param("excluded")   ShowTimeStatus excluded
    );

    // ── Kiểm tra conflict lịch chiếu cùng phòng ──────────────────────
    // Dùng khi tạo / chỉnh suất: đảm bảo không overlap với buffer 20 phút

    @Query("""
            SELECT COUNT(s) > 0 FROM Showtime s
              JOIN s.movie m
            WHERE s.room.id   = :roomId
              AND s.id        != :excludeId
              AND s.status   NOT IN ('CANCELLED')
              AND s.deletedAt IS NULL
              AND :newStart   < (s.startTime + (m.duration + 20) * 60 second)
              AND :newEnd     > s.startTime
            """)
    boolean existsConflict(
            @Param("roomId")    String roomId,
            @Param("excludeId") String excludeId,   // 0L khi tạo mới
            @Param("newStart")  LocalDateTime newStart,
            @Param("newEnd")    LocalDateTime newEnd
    );

    // ── Các suất cần tự động chuyển trạng thái (dùng bởi scheduler) ──

    @Query("""
            SELECT s FROM Showtime s
              JOIN FETCH s.movie m
            WHERE s.status    = 'SCHEDULED'
              AND s.startTime <= :now
              AND s.deletedAt IS NULL
            """)
    List<Showtime> findScheduledShowtimesToStart(@Param("now") LocalDateTime now);

    @Query("""
            SELECT s FROM Showtime s
              JOIN FETCH s.movie m
            WHERE s.status    = 'ONGOING'
              AND s.deletedAt IS NULL
            """)
    List<Showtime> findAllOngoing();

    @Modifying
    @Query("""
            UPDATE Showtime s
            SET s.availableSeats = (
                SELECT COUNT(ss) FROM ShowtimeSeat ss
                WHERE ss.showtime.id = :showtimeId
                  AND ss.status = org.example.cinemaBooking.Shared.enums.SeatStatus.AVAILABLE
                  AND ss.deletedAt IS NULL
            )
            WHERE s.id = :showtimeId
            """)
    void syncAvailableSeats(@Param("showtimeId") String showtimeId);


    @Query("""
                SELECT COUNT(s)
                FROM Showtime s
                WHERE s.startTime >= :start
                AND s.startTime < :end
                AND s.deletedAt IS NULL
            """)
    int countShowtimes(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}