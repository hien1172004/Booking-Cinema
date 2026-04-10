package org.example.cinemaBooking.Entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.cinemaBooking.Shared.persistence.SoftDeletableEntity;
import org.example.cinemaBooking.Shared.enums.AgeRating;
import org.example.cinemaBooking.Shared.enums.MovieStatus;


import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
//@Where(clause = "deleted = false")
public class Movie extends SoftDeletableEntity {

    @Column (nullable = false)
    String title;

    @Column (nullable = false, unique = true, length = 100)
    String slug;

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(nullable = false)
    Integer duration;

    LocalDate releaseDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    AgeRating ageRating = AgeRating.P;

    @Column(nullable = false)
    String language;

    @Column(nullable = false, length = 500)
    String posterUrl;

    @Column(nullable = false)
    String trailerUrl;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    MovieStatus status = MovieStatus.COMING_SOON;


    @ManyToMany
    @JoinTable(
            name = "movie_category",
            joinColumns = @JoinColumn(name = "movie_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    @Builder.Default
    Set<Category> categories = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true)
    List<MovieImage> images = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true)
    List<MoviePeople> moviePeoples = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "movie")
    List<Review> reviews = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "movie")
    List<Showtime> showtimes = new ArrayList<>();
}
