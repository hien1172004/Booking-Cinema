package org.example.cinemaBooking.DTO.Response.Movie;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.cinemaBooking.DTO.Response.CategoryResponse;
import org.example.cinemaBooking.Shared.enums.AgeRating;
import org.example.cinemaBooking.Shared.enums.MovieStatus;

import java.time.LocalDate;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MovieResponse {

    String id;

    String title;

    String slug;

    String description;

    Integer duration;

    LocalDate releaseDate;

    AgeRating ageRating;

    String language;

    String posterUrl;

    String trailerUrl;

    MovieStatus status;

    Set<CategoryResponse> categories;
}