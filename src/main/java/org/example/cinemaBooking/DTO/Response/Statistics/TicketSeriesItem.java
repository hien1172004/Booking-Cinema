package org.example.cinemaBooking.DTO.Response.Statistics;

import lombok.*;

import java.time.LocalDate;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class TicketSeriesItem{
    LocalDate date;
    Long count;
}