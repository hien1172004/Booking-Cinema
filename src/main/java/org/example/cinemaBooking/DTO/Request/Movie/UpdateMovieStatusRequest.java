package org.example.cinemaBooking.DTO.Request.Movie;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.cinemaBooking.Shared.constraints.EnumValidator;
import org.example.cinemaBooking.Shared.enums.MovieStatus;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateMovieStatusRequest {
    @EnumValidator(enumClass = MovieStatus.class, message = "MOVIE_STATUS_INVALID")
    String status;
}
