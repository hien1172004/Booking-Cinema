package org.example.cinemaBooking.Entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.cinemaBooking.Shared.persistence.SoftDeletableEntity;
import org.example.cinemaBooking.Shared.enums.Type;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Notification extends SoftDeletableEntity {
    @Column(nullable = false)
    String title;
    @Column(columnDefinition = "TEXT")
    String body;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    Type type = Type.BOOKING;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    boolean read = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    UserEntity user;

}
