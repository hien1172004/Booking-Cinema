package org.example.cinemaBooking.Entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.cinemaBooking.Shared.persistence.SoftDeletableEntity;
import org.example.cinemaBooking.Shared.enums.RoomType;
import org.example.cinemaBooking.Shared.enums.Status;

import java.util.ArrayList;
import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class Room extends SoftDeletableEntity {
    @Column(nullable = false, unique = true)
    String name;

    @Column(nullable = false)
    Integer totalSeats;

    @Builder.Default
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    RoomType roomType = RoomType.TWO_D;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    Status status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cinema_id", nullable = false)
    Cinema cinema;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Seat> seats = new ArrayList<>();
}
