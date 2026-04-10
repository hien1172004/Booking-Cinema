package org.example.cinemaBooking.DTO.Response.Statistics;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class RevenueSeriesItem{
    LocalDate date;
    BigDecimal value;
}