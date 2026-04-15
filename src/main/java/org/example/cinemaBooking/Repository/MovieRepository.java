package org.example.cinemaBooking.Repository;

import jakarta.validation.constraints.NotBlank;
import org.example.cinemaBooking.DTO.Response.Movie.MovieStats;
import org.example.cinemaBooking.Entity.Movie;
import org.example.cinemaBooking.Shared.enums.MovieStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MovieRepository extends JpaRepository<Movie, String>,
        JpaSpecificationExecutor<Movie> {

    @Override
    @EntityGraph(attributePaths = {"categories"})
    Page<Movie> findAll(Specification<Movie> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"categories"})
    Optional<Movie> findByIdAndDeletedFalse(String id);

    @EntityGraph(attributePaths = {"categories"})
    Optional<Movie> findBySlugAndDeletedFalse(String slug);

    @EntityGraph(attributePaths = {"categories"})
    Page<Movie> findByStatusAndDeletedFalse(MovieStatus movieStatus, Pageable pageable);

    @EntityGraph(attributePaths = {"categories"})
    @Query("""
SELECT DISTINCT m FROM Movie m
WHERE (:key IS NULL OR LOWER(m.title) LIKE LOWER(CONCAT('%', :key, '%')))
AND m.status IN ('COMING_SOON', 'NOW_SHOWING') 
AND m.deleted = false
""")
    Page<Movie> searchMovie(String key, Pageable pageable);

    Optional<Movie> findBySlug(@NotBlank(message = "SLUG_REQUIRED") String slug);

    boolean existsBySlug(String slug);


    @Query("""
    SELECT new org.example.cinemaBooking.DTO.Response.Movie.MovieStats(
        m.id, 
        m.title, 
        m.posterUrl, 
        CAST(COALESCE(SUM(t.price), 0) AS double), 
        COUNT(t.id), 
        (SELECT CAST(COALESCE(AVG(r.rating), 0.0) AS double) FROM Review r WHERE r.movie = m)
    )
    FROM Movie m
    LEFT JOIN Showtime s ON s.movie = m
    LEFT JOIN Booking b ON b.showtime = s AND b.status = 'CONFIRMED'
    LEFT JOIN Ticket t ON t.booking = b
    WHERE m.status = 'NOW_SHOWING'
    GROUP BY m.id, m.title, m.posterUrl
""")
    List<MovieStats> getMovieStats();


    @Modifying
    @Query("""
    UPDATE Movie m
    SET m.status = :newStatus
    WHERE m.status = :oldStatus
      AND m.releaseDate <= :today
""")
    int updateStatusToNowShowing(
            MovieStatus newStatus,
            MovieStatus oldStatus,
            LocalDate today
    );

    /**
     * Tìm phim đang chiếu / sắp chiếu theo tên thể loại (dùng cho chatbot).
     */
    @EntityGraph(attributePaths = {"categories"})
    @Query("""
    SELECT DISTINCT m FROM Movie m
    JOIN m.categories c
    WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :genreName, '%'))
      AND m.status IN ('NOW_SHOWING', 'COMING_SOON')
      AND m.deleted = false
    ORDER BY m.releaseDate DESC
    """)
    List<Movie> findByGenreName(@Param("genreName") String genreName);

    /**
     * Tìm phim có diễn viên hoặc đạo diễn tham gia theo tên (dùng cho chatbot).
     */
    @EntityGraph(attributePaths = {"categories"})
    @Query("""
    SELECT DISTINCT m FROM Movie m
    JOIN MoviePeople mp ON mp.movie = m
    JOIN mp.people p
    WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :actorName, '%'))
      AND m.status IN ('NOW_SHOWING', 'COMING_SOON')
      AND m.deleted = false
    ORDER BY m.releaseDate DESC
    """)
    List<Movie> findByActorName(@Param("actorName") String actorName);
}
