package org.example.cinemaBooking.Entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.cinemaBooking.Shared.persistence.SoftDeletableEntity;
import org.example.cinemaBooking.Shared.enums.SeatStatus;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(
        name = "showtime_seat",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_showtime_seat",
                columnNames = {"showtime_id", "seat_id"}
        )
)
@Setter
@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShowtimeSeat extends SoftDeletableEntity {

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    SeatStatus status = SeatStatus.AVAILABLE;   // trạng thái ghế: AVAILABLE, BOOKED, LOCKED

    private LocalDateTime lockedUntil;   // hết hạn giữ chỗ tạm


    //sua tu long thanh sang string
    private String lockedByUser;           // FK user đang giữ (không hard FK để tránh deadlock)


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "showtime_id", nullable = false)
    private Showtime showtime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;
}
