package org.example.cinemaBooking.DTO.Response.Movie;

import java.time.LocalDate;

public record PeopleResponse(
        String id,
        String name,
        String nation,
        String avatarUrl,
        LocalDate dob
) {}