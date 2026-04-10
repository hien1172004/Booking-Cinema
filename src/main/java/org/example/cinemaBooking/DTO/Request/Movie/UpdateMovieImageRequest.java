package org.example.cinemaBooking.DTO.Request.Movie;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateMovieImageRequest {

    @NotEmpty(message = "IMAGE_URL_LIST_REQUIRED")
    List<@NotBlank(message = "IMAGE_URL_REQUIRED") String> imageUrls;
}