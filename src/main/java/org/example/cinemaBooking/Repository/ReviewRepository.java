package org.example.cinemaBooking.Repository;

import org.example.cinemaBooking.Entity.Review;
import org.springframework.data.repository.query.Param;
import org.example.cinemaBooking.Entity.Movie;

import org.example.cinemaBooking.Entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, String> {
    Optional<Review> findByMovieAndUser(Movie movie, UserEntity user);

    @EntityGraph(attributePaths = {"movie", "user"})
    Optional<Review> findWithMovieAndUserById(String id);

    @EntityGraph(attributePaths = {"user"})
    Page<Review> findByMovieIdAndDeletedFalse(String movieId, Pageable pageable);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.movie.id = :movieId AND r.deleted = false")
    Double getAverageRatingByMovieId(@Param("movieId") String movieId);

    long countByMovieIdAndDeletedFalse(String movieId);

    @EntityGraph(attributePaths = {"user"})
    Page<Review> findByMovieIdAndRatingGreaterThanEqualAndDeletedFalse(String movieId, int minimumRating, Pageable pageable);
}
