package org.example.cinemaBooking.Entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.cinemaBooking.Shared.persistence.SoftDeletableEntity;

@Entity
@Table(name = "used_promotion",
        indexes = {
                @Index(name = "idx_user_promotion", columnList = "user_id,promotion_id")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "unique_user_promotion",
                columnNames = {"user_id", "promotion_id"}
        )
)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UsedPromotion extends SoftDeletableEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotion_id", nullable = false)
    Promotion promotion;

    @Column(nullable = false)
    @Builder.Default
    int usageCount = 0;
}
