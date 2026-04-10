package org.example.cinemaBooking.DTO.Request.Movie;

import java.io.Serializable;
import java.time.LocalDate;

public record UpdatePeopleRequest(
        String name,
        String nation,
        String avatarUrl,
        LocalDate dob
) implements Serializable {}