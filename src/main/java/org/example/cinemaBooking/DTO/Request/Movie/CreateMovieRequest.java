package org.example.cinemaBooking.DTO.Request.Movie;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.cinemaBooking.Shared.constraints.EnumValidator;
import org.example.cinemaBooking.Shared.enums.AgeRating;

import java.time.LocalDate;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateMovieRequest {

    @NotBlank(message = "TITLE_REQUIRED")
    String title;

    @NotBlank(message = "SLUG_REQUIRED")
    String slug;

    String description;

    @NotNull(message = "DURATION_REQUIRED")
    Integer duration;

    @NotNull(message = "RELEASE_DATE_REQUIRED")
    LocalDate releaseDate;

    @EnumValidator(enumClass = AgeRating.class, message = "INVALID_AGE_RATING")
    String ageRating;

    @NotBlank(message = "LANGUAGE_REQUIRED")
    String language;

    @NotBlank(message = "POSTER_URL_REQUIRED")
    String posterUrl;
    @NotBlank(message = "TRAILER_URL_REQUIRED")
    String trailerUrl;

    @NotEmpty(message = "CATEGORIES_REQUIRED")
    Set<String> categoryIds;
}