// ─────────────────────────────────────────────────────────────────
// PaymentRepository.java
// ─────────────────────────────────────────────────────────────────
package org.example.cinemaBooking.Repository;

import org.example.cinemaBooking.DTO.Response.Statistics.RevenueSeriesItem;
import org.example.cinemaBooking.DTO.Response.Statistics.TimeSeriesItem;
import org.example.cinemaBooking.Entity.Payment;
import org.example.cinemaBooking.Shared.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

    // FIX P7: thêm JOIN FETCH để tránh LazyInitializationException
    @Query("""
        SELECT p FROM Payment p
        JOIN FETCH p.booking b
        WHERE b.id = :bookingId
          AND p.deletedAt IS NULL
        """)
    Optional<Payment> findByBookingId(@Param("bookingId") String bookingId);

    /**
     * Dùng cho refund — cần JOIN FETCH đầy đủ để cancel booking + release ghế
     */
    @Query("""
        SELECT p FROM Payment p
        JOIN FETCH p.booking b
        JOIN FETCH b.showtime st
        JOIN FETCH st.movie
        LEFT JOIN FETCH b.tickets t
        LEFT JOIN FETCH t.seat s
        LEFT JOIN FETCH s.seatType
        WHERE b.id = :bookingId
          AND p.deletedAt IS NULL
        """)
    Optional<Payment> findByBookingIdWithDetails(@Param("bookingId") String bookingId);

    Optional<Payment> findByTransactionId(String transactionId);

    /** IPN handler dùng — tìm theo bookingCode (vnp_TxnRef) */
    @Query("""
        SELECT p FROM Payment p
        JOIN FETCH p.booking b
        JOIN FETCH b.showtime st
        JOIN FETCH st.movie
        LEFT JOIN FETCH b.tickets t
        LEFT JOIN FETCH t.seat
        WHERE b.bookingCode = :bookingCode
          AND p.deletedAt IS NULL
        """)
    Optional<Payment> findByBookingCode(@Param("bookingCode") String bookingCode);


    @Query("""
    SELECT COALESCE(SUM(p.amount), 0)
    FROM Payment p
    WHERE p.status = :status
    AND p.createdAt >= :start
    AND p.createdAt < :end
""")
    BigDecimal getRevenue(
            @Param("status") PaymentStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );


//    @Query("""
//    SELECT new org.example.cinemaBooking.DTO.Response.Statistics.TimeSeriesItem(
//        FUNCTION('DATE', p.createdAt),
//        COALESCE(SUM(p.amount), 0)
//    )
//    FROM Payment p
//    WHERE p.status = :status
//    AND p.createdAt >= :start
//    AND p.createdAt < :end
//    GROUP BY FUNCTION('DATE', p.createdAt)
//    ORDER BY FUNCTION('DATE', p.createdAt)
//""")
//    List<TimeSeriesItem<BigDecimal>> getRevenueSeries(
//            PaymentStatus status,
//            LocalDateTime start,
//            LocalDateTime end
//    );

//    Tổng doanh thu (có filter cinema optional)
    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM Payment p
        JOIN p.booking b
        JOIN b.showtime s
        JOIN s.room r
        JOIN r.cinema c
        WHERE p.status = :status
          AND p.createdAt >= :start
          AND p.createdAt < :end
          AND (:cinemaId IS NULL OR c.id = :cinemaId)
    """)
    BigDecimal getRevenue(
            @Param("status") PaymentStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("cinemaId") String cinemaId
    );

    // Revenue theo ngày (time series)
    @Query("""
        SELECT new org.example.cinemaBooking.DTO.Response.Statistics.RevenueSeriesItem(
             CAST(p.createdAt AS LocalDate),
            COALESCE(SUM(p.amount), 0)
        )
        FROM Payment p
        JOIN p.booking b
        JOIN b.showtime s
        JOIN s.room r
        JOIN r.cinema c
        WHERE p.status = :status
          AND p.createdAt >= :start
          AND p.createdAt < :end
          AND (:cinemaId IS NULL OR c.id = :cinemaId)
        GROUP BY FUNCTION('DATE', p.createdAt)
        ORDER BY FUNCTION('DATE', p.createdAt)
    """)
    List<RevenueSeriesItem> getRevenueSeries(
            @Param("status") PaymentStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("cinemaId") String cinemaId
    );

}
