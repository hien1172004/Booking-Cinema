package org.example.cinemaBooking.Repository;

import org.example.cinemaBooking.DTO.Response.Cinema.CinemaMovieResponse;
import org.example.cinemaBooking.Entity.Cinema;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CinemaRepository extends JpaRepository<Cinema, String> {
    List<Cinema> findCinemaByName(String name);

    Cinema findCinemaById(String id);

    Page<Cinema> findByNameContainingIgnoreCaseAndDeletedFalse(String keyword, Pageable pageable);

    Page<Cinema> findAllByDeletedFalse(Pageable pageable);

    @Query("""
        SELECT DISTINCT new org.example.cinemaBooking.DTO.Response.Cinema.CinemaMovieResponse(
            s.movie.id,
            s.movie.title,
            s.movie.posterUrl,
            s.movie.duration,
            s.language
        )
        FROM Showtime s
        JOIN s.room r
        WHERE r.cinema.id = :cinemaId
          AND s.startTime >= :from
          AND s.startTime < :to
          AND s.status <> 'CANCELLED'
          AND s.deletedAt IS NULL
    """)
    Page<CinemaMovieResponse> findMoviesByCinemaAndDate(
            @Param("cinemaId") String cinemaId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    /**
     * Tìm rạp theo tên (LIKE, không phân biệt hoa thường) — dùng cho chatbot.
     */
    List<Cinema> findByNameContainingIgnoreCaseAndDeletedFalse(String keyword);
}
