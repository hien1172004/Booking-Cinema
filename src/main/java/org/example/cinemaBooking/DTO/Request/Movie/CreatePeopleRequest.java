package org.example.cinemaBooking.DTO.Request.Movie;

import jakarta.validation.constraints.NotBlank;


import java.io.Serializable;
import java.time.LocalDate;

/**
 * DTO for {@link org.example.cinemaBooking.Entity.Movie.People}
 */
public record CreatePeopleRequest(@NotBlank(message = "NAME_REQUIRED") String name,
                                  @NotBlank(message = "NATION_REQUIRED") String nation,
                                  @NotBlank(message = "IMAGE_URL_NOT_BLANK") String avatarUrl,
                                  LocalDate dob)
        implements Serializable {
}