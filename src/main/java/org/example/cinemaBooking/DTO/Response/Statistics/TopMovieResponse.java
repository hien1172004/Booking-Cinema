    package org.example.cinemaBooking.DTO.Response.Statistics;

    import java.math.BigDecimal;

    public record TopMovieResponse(
        String movieName,
        String imageUrl,
        Long tickets,
        BigDecimal revenue
    ) {}