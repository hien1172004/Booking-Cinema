package org.example.cinemaBooking.Repository;


import org.example.cinemaBooking.Entity.MoviePeople;
import org.example.cinemaBooking.Shared.enums.MovieRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface MoviePeopleRepository extends JpaRepository<MoviePeople, String> {
    boolean existsByPeopleId(String id);

    @Query("""
SELECT mp FROM MoviePeople mp
JOIN FETCH mp.movie m
WHERE mp.people.id = :peopleId
""")
    List<MoviePeople> findByPeopleId(String peopleId);

    boolean existsByMovieIdAndPeopleId(String moveId, String peopleId);

    @Query("""
SELECT mp FROM MoviePeople mp
JOIN FETCH mp.people p
WHERE mp.movie.id = :movieId
""")
    List<MoviePeople> findByMovieId(String movieId);

    Optional<MoviePeople> findByMovieIdAndPeopleId(String movieId, String peopleId);

    void deleteByPeopleId(String peopleId);

    List<MoviePeople> findByMovieIdAndMovieRole(String movieId, MovieRole movieRole);

    @Query("SELECT mp.people.id FROM MoviePeople mp WHERE mp.movie.id = :movieId")
    Set<String> findPeopleIdsByMovieId(String movieId);

    @Modifying
    @Query("DELETE FROM MoviePeople mp WHERE mp.movie.id = :movieId AND mp.people.id = :peopleId")
    int deleteByMovieIdAndPeopleId(String movieId, String peopleId);

    void deleteByMovieId(String movieId);

    long countByMovieIdAndMovieRole(String movieId, MovieRole movieRole);
}
