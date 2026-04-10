package org.example.cinemaBooking.Repository;

import org.example.cinemaBooking.Entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, String> {

    @Query("""
    SELECT s FROM Seat s
    JOIN FETCH s.seatType
    WHERE s.room.id = :roomId AND s.deletedAt IS NULL
     ORDER BY s.seatRow ASC, s.seatNumber ASC
""")
    List<Seat> findAllByRoomIdFetch(String roomId);

    Optional<Seat> findByRoomIdAndSeatRowAndSeatNumberAndDeletedAtIsNull(
            String roomId, String seatRow, Integer seatNumber
    );

    boolean existsByRoomIdAndSeatRowAndSeatNumberAndDeletedAtIsNull(
            String roomId, String seatRow, Integer seatNumber
    );
}
