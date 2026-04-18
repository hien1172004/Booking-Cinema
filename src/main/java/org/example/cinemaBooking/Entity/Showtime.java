package org.example.cinemaBooking.Entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import org.example.cinemaBooking.Shared.persistence.SoftDeletableEntity;
import org.example.cinemaBooking.Shared.enums.Language;
import org.example.cinemaBooking.Shared.enums.ShowTimeStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Showtime extends SoftDeletableEntity {

    @Column(nullable = false)
    LocalDateTime startTime;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    Language language = Language.SUBTITLED;

    @Column(nullable = false)
    @Builder.Default
    private Integer availableSeats = 0; // cache

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    ShowTimeStatus status = ShowTimeStatus.SCHEDULED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @OneToMany(mappedBy = "showtime", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ShowtimeSeat> showtimeSeats = new ArrayList<>();

    // ── Computed helpers ─────────────────────────────────────────────
    private static final int BUFFER_MINUTES = 20;

    public LocalDateTime getEndTime() {
        return startTime.plusMinutes(movie.getDuration() + BUFFER_MINUTES);
    }

    public boolean isOngoing() {
        LocalDateTime now = LocalDateTime.now();
        return status == ShowTimeStatus.ONGOING
                && now.isAfter(startTime)
                && now.isBefore(getEndTime());
    }

    public boolean isFinished() {
        return LocalDateTime.now().isAfter(getEndTime());
    }

    public boolean isBookable() {
        return status == ShowTimeStatus.SCHEDULED
                && LocalDateTime.now().isBefore(startTime.minusMinutes(5));
    }

    public boolean isCancelled() {
        return status == ShowTimeStatus.CANCELLED;
    }

}
