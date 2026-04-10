package org.example.cinemaBooking.DTO.Response.Statistics;



import java.time.LocalDate;

public record TimeSeriesItem<T>(
        LocalDate date,
        T value
) {}