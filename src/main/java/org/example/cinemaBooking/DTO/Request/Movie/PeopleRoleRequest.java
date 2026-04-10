package org.example.cinemaBooking.DTO.Request.Movie;

import jakarta.validation.constraints.NotBlank;
import org.example.cinemaBooking.Shared.contraints.EnumValidator;
import org.example.cinemaBooking.Shared.enums.MovieRole;

public record PeopleRoleRequest(
        @NotBlank(message = "PEOPLE_ID_REQUIRED") String peopleId,
        @NotBlank(message = "ROLE_REQUIRED") @EnumValidator(enumClass = MovieRole.class)String role
) {}