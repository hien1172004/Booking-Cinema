package org.example.cinemaBooking.Entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.cinemaBooking.Shared.persistence.SoftDeletableEntity;


@Table(
        name = "reviews",  // Nên đặt tên bảng rõ ràng
        uniqueConstraints = @UniqueConstraint(
                name = "uk_review_movie_user",  // Đặt tên constraint rõ ràng
                columnNames = {"movie_id", "user_id"}
        ),
        indexes = {
                @Index(name = "idx_review_movie", columnList = "movie_id"),
                @Index(name = "idx_review_user", columnList = "user_id"),
                @Index(name = "idx_review_rating", columnList = "rating") // Thêm nếu hay sort theo rating
        }
)
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Review extends SoftDeletableEntity {
    @Column(columnDefinition = "TEXT")
    String comment;

    @Column(nullable = false)
    int rating; // 1-> 10

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    Movie movie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    UserEntity user;
}
