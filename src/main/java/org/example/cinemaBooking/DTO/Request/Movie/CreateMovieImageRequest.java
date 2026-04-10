package org.example.cinemaBooking.DTO.Request.Movie;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateMovieImageRequest {
    @NotNull(message = "IMAGE_URL_IS_REQUIRED")
    List<String > imageUrls;
}
