package org.example.cinemaBooking.DTO.Response.Statistics;

import lombok.Builder;
import lombok.Data;

import java.util.List;

public record TimeSeriesResponse<T>(
        List<TimeSeriesItem<T>> data
) {}