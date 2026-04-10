package org.example.cinemaBooking.Repository;



import org.example.cinemaBooking.Entity.Cinema;
import org.example.cinemaBooking.Entity.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface RoomRepository extends JpaRepository<Room, String> {

    Page<Room> findByNameContainingIgnoreCaseAndDeletedFalse(String keyword, Pageable pageable);
    Page<Room> findAllByDeletedFalse(Pageable deleted);

    @Query("SELECT r FROM Room r LEFT JOIN FETCH r.seats WHERE r.id = :id")
    Optional<Room> findByIdWithSeats(@Param("id") String id);

    Page<Room> findByCinemaId(String cinemaId, Pageable pageable);

    Page<Room> findRoomsByCinema(Cinema cinema, Pageable pageable);
}
