package org.example.cinemaBooking.Repository;


import org.example.cinemaBooking.Entity.MovieImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MovieImageRepository extends JpaRepository<MovieImage, String> {
    List<MovieImage> findByMovieId(String movieId);

    void deleteByMovieId(String movieId);

    Optional<MovieImage> findByIdAndMovieId(String imageId, String movieId);
}
