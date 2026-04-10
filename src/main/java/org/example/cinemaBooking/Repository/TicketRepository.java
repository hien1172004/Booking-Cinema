package org.example.cinemaBooking.Repository;

import org.example.cinemaBooking.DTO.Response.Statistics.TicketSeriesItem;
import org.example.cinemaBooking.DTO.Response.Statistics.TimeSeriesItem;
import org.example.cinemaBooking.DTO.Response.Statistics.TopMovieResponse;
import org.example.cinemaBooking.Entity.Ticket;
import org.example.cinemaBooking.Shared.enums.PaymentStatus;
import org.example.cinemaBooking.Shared.enums.TicketStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, String> {
    @Query("""
            SELECT t FROM Ticket t
            JOIN FETCH t.seat s
            JOIN FETCH s.seatType
            WHERE t.booking.id = :bookingId AND t.deletedAt IS NULL
            """)
    List<Ticket> findAllByBookingId(@Param("bookingId") String bookingId);

    Optional<Ticket> findByTicketCode(String ticketCode);


    @Query("""
            SELECT t FROM Ticket t
            JOIN FETCH t.seat s
            JOIN FETCH s.seatType
            JOIN FETCH t.booking b
            JOIN FETCH b.showtime st
            JOIN FETCH st.movie
            JOIN FETCH st.room r
            JOIN FETCH r.cinema
            WHERE t.ticketCode = :ticketCode
              AND t.deletedAt IS NULL
            """)
    Optional<Ticket> findByTicketCodeWithDetails(@Param("ticketCode") String ticketCode);

    /**
     * Scheduled job — expire vé chưa dùng sau khi suất chiếu kết thúc
     */
    @Query("""
            SELECT t FROM Ticket t
            JOIN FETCH t.booking b
            JOIN FETCH b.showtime st
            WHERE t.status     = org.example.cinemaBooking.Shared.enums.TicketStatus.VALID
              AND st.status    = org.example.cinemaBooking.Shared.enums.ShowTimeStatus.FINISHED
              AND t.deletedAt IS NULL
            """)
    List<Ticket> findValidTicketsOfFinishedShowtimes();

    /**
     * Lấy tất cả vé của 1 user
     */
    @Query("""
            SELECT t FROM Ticket t
            JOIN FETCH t.seat s
            JOIN FETCH s.seatType
            JOIN FETCH t.booking b
            JOIN FETCH b.showtime st
            JOIN FETCH st.movie
            JOIN FETCH st.room r
            JOIN FETCH r.cinema
            WHERE b.user.id   = :userId
              AND t.deletedAt IS NULL
            ORDER BY st.startTime DESC
            """)
    List<Ticket> findAllByUserId(@Param("userId") String userId);

    @Query("""
            SELECT t FROM Ticket t
            JOIN FETCH t.seat s
            JOIN FETCH s.seatType
            JOIN FETCH t.booking b
            JOIN FETCH b.showtime st
            JOIN FETCH st.movie
            JOIN FETCH st.room r
            WHERE b.bookingCode = :bookingCode
              AND t.deletedAt IS NULL
            """)
    List<Ticket> findAllByBookingCode(@Param("bookingCode") String bookingCode);


//    @Query("""
//                SELECT COUNT(t)
//                FROM Ticket t
//                WHERE t.status     = :status
//                  AND t.createdAt >= :start
//                  AND t.createdAt <  :end
//                  AND t.deletedAt IS NULL
//            """)
//    int countTickets(@Param("status") TicketStatus status,
//                     @Param("start") LocalDateTime start,
//                     @Param("end") LocalDateTime end);


//    @Query("""
//                SELECT new org.example.cinemaBooking.DTO.Response.Statistics.TopMovieResponse(
//                    m.title,
//                    m.posterUrl,
//                    COUNT(t),
//                    SUM(t.price)
//                )
//                FROM Ticket t
//                JOIN t.booking b
//                JOIN b.payment p
//                JOIN b.showtime s
//                JOIN s.movie m
//                WHERE p.status = :status
//                AND p.createdAt >= :start
//                AND p.createdAt < :end
//                GROUP BY m.id, m.title, m.posterUrl
//                ORDER BY SUM(t.price) DESC
//            """)
//    List<TopMovieResponse> getTopMovies(
//            PaymentStatus status,
//            LocalDateTime start,
//            LocalDateTime end,
//            Pageable pageable
//    );

    // Tổng vé bán (có filter cinema + movie)
    @Query("""
        SELECT COUNT(t)
        FROM Ticket t
        JOIN t.booking b
        JOIN b.showtime s
        JOIN s.movie m
        JOIN s.room r
        JOIN r.cinema c
        WHERE t.status = :status
          AND t.createdAt >= :start
          AND t.createdAt < :end
          AND (:cinemaId IS NULL OR c.id = :cinemaId)
          AND (:movieId IS NULL OR m.id = :movieId)
    """)
    int countTickets(
            @Param("status") TicketStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("cinemaId") String cinemaId,
            @Param("movieId") String movieId
    );

    @Query("""
        SELECT COUNT(DISTINCT b.id)
        FROM Ticket t
        JOIN t.booking b
        JOIN b.showtime s
        JOIN s.movie m
        JOIN s.room r
        JOIN r.cinema c
        WHERE t.status = :status
          AND t.createdAt >= :start
          AND t.createdAt < :end
          AND (:cinemaId IS NULL OR c.id = :cinemaId)
          AND (:movieId IS NULL OR m.id = :movieId)
    """)
    int countBookings(
            @Param("status") TicketStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("cinemaId") String cinemaId,
            @Param("movieId") String movieId
    );

    @Query("""
        SELECT COUNT(DISTINCT m.id)
        FROM Ticket t
        JOIN t.booking b
        JOIN b.showtime s
        JOIN s.movie m
        JOIN s.room r
        JOIN r.cinema c
        WHERE t.status = :status
          AND t.createdAt >= :start
          AND t.createdAt < :end
          AND (:cinemaId IS NULL OR c.id = :cinemaId)
          AND (:movieId IS NULL OR m.id = :movieId)
    """)
    int countMovies(
            @Param("status") TicketStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("cinemaId") String cinemaId,
            @Param("movieId") String movieId
    );

    // Ticket series (theo ngày)
    @Query("""
        SELECT new org.example.cinemaBooking.DTO.Response.Statistics.TicketSeriesItem(
            CAST(t.createdAt AS LocalDate),
            COUNT(t)
        )
        FROM Ticket t
        JOIN t.booking b
        JOIN b.showtime s
        JOIN s.movie m
        JOIN s.room r
        JOIN r.cinema c
        WHERE t.status = :status
          AND t.createdAt >= :start
          AND t.createdAt < :end
          AND (:cinemaId IS NULL OR c.id = :cinemaId)
          AND (:movieId IS NULL OR m.id = :movieId)
        GROUP BY FUNCTION('DATE', t.createdAt)
        ORDER BY FUNCTION('DATE', t.createdAt)
    """)
    List<TicketSeriesItem> getTicketSeries(
            @Param("status") TicketStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("cinemaId") String cinemaId,
            @Param("movieId") String movieId
    );

    // Top movies theo doanh thu
    @Query("""
        SELECT new org.example.cinemaBooking.DTO.Response.Statistics.TopMovieResponse(
            m.title,
            m.posterUrl,
            COUNT(t),
            SUM(t.price)
        )
        FROM Ticket t
        JOIN t.booking b
        JOIN b.showtime s
        JOIN s.movie m
        JOIN b.payment p
        JOIN s.room r
        JOIN r.cinema c
        WHERE p.status = :status
          AND t.createdAt >= :start
          AND t.createdAt < :end
          AND (:cinemaId IS NULL OR c.id = :cinemaId)
        GROUP BY m.id, m.title, m.posterUrl
        ORDER BY SUM(t.price) DESC
    """)
    List<TopMovieResponse> getTopMovies(
            @Param("status") PaymentStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("cinemaId") String cinemaId,
            Pageable pageable
    );
}
