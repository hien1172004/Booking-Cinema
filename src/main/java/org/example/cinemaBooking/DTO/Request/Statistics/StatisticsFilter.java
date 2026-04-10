package org.example.cinemaBooking.DTO.Request.Statistics;

import lombok.Data;

import java.time.LocalDate;

@Data
public class StatisticsFilter {
    private LocalDate from;
    private LocalDate to;
    private Long movieId;
    private Long cinemaId;
}