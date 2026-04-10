package org.example.cinemaBooking.Entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.cinemaBooking.Shared.persistence.SoftDeletableEntity;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class Seat extends SoftDeletableEntity {
    @Column(nullable = false, length = 2)
    String seatRow; // A, B, C, D, E, F, G, H, I, J

    @Column(nullable = false, length = 2)
    Integer seatNumber; // 1, 2, 3, 4, 5, 6, 7, 8, 9, 10

    @Builder.Default
    @Column(nullable = false)
    boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    Room room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_type_id", nullable = false)
    SeatType seatType;
}
