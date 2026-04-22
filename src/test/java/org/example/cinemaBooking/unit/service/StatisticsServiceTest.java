package org.example.cinemaBooking.unit.service;

import org.example.cinemaBooking.DTO.Response.Statistics.DashboardSummaryResponse;
import org.example.cinemaBooking.DTO.Response.Statistics.RevenueSeriesItem;
import org.example.cinemaBooking.DTO.Response.Statistics.TicketSeriesItem;
import org.example.cinemaBooking.DTO.Response.Statistics.TopMovieResponse;
import org.example.cinemaBooking.Repository.PaymentRepository;
import org.example.cinemaBooking.Repository.TicketRepository;
import org.example.cinemaBooking.Service.Statistics.StatisticsService;
import org.example.cinemaBooking.Shared.enums.PaymentStatus;
import org.example.cinemaBooking.Shared.enums.TicketStatus;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class StatisticsServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private StatisticsService statisticsService;

    @Nested
    class SummaryTests {
        @Test
        void getSummary_Success() {
            // Given
            LocalDate from = LocalDate.now().minusDays(7);
            LocalDate to = LocalDate.now();
            when(paymentRepository.getRevenue(eq(PaymentStatus.SUCCESS), any(), any(), any()))
                    .thenReturn(BigDecimal.valueOf(1000000));
            when(ticketRepository.countTickets(any(), any(), any(), any(), any()))
                    .thenReturn(50);

            // When
            DashboardSummaryResponse summary = statisticsService.getSummary(from, to, null, null);

            // Then
            assertThat(summary.revenueToday()).isEqualTo(BigDecimal.valueOf(1000000));
            assertThat(summary.ticketsToday()).isEqualTo(50);
        }
    }

    @Nested
    class ChartTests {
        @Test
        void getRevenueChart_FillsMissingDates() {
            // Given
            LocalDate from = LocalDate.now().minusDays(2); // D-2, D-1, Today
            LocalDate to = LocalDate.now();
            List<RevenueSeriesItem> raw = new ArrayList<>();
            raw.add(new RevenueSeriesItem(from, BigDecimal.valueOf(100)));
            raw.add(new RevenueSeriesItem(to, BigDecimal.valueOf(300)));

            when(paymentRepository.getRevenueSeries(any(), any(), any(), any())).thenReturn(raw);

            // When
            List<RevenueSeriesItem> result = statisticsService.getRevenueChart(from, to, null);

            // Then
            assertThat(result).hasSize(3);
            assertThat(result.get(0).getValue()).isEqualTo(BigDecimal.valueOf(100)); // D-2
            assertThat(result.get(1).getValue()).isEqualTo(BigDecimal.ZERO);      // D-1 (Filled)
            assertThat(result.get(2).getValue()).isEqualTo(BigDecimal.valueOf(300)); // Today
        }

        @Test
        void getTicketChart_FillsMissingDates() {
            // Given
            LocalDate from = LocalDate.now().minusDays(2);
            LocalDate to = LocalDate.now();
            List<TicketSeriesItem> raw = new ArrayList<>();
            raw.add(new TicketSeriesItem(from, 5L));

            when(ticketRepository.getTicketSeries(any(), any(), any(), any(), any())).thenReturn(raw);

            // When
            List<TicketSeriesItem> result = statisticsService.getTicketChart(from, to, null, null);

            // Then
            assertThat(result).hasSize(3);
            assertThat(result.get(0).getCount()).isEqualTo(5L);
            assertThat(result.get(1).getCount()).isEqualTo(0L); // Filled
            assertThat(result.get(2).getCount()).isEqualTo(0L); // Filled
        }
    }

    @Nested
    class TopMoviesTests {
        @Test
        void getTopMovies_Success() {
            // Given
            LocalDate from = LocalDate.now().minusDays(30);
            LocalDate to = LocalDate.now();
            TopMovieResponse mockMovie = new TopMovieResponse("Movie A", "img", 100L, BigDecimal.valueOf(1000000));

            when(ticketRepository.getTopMovies(any(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(Arrays.asList(mockMovie));

            // When
            List<TopMovieResponse> result = statisticsService.getTopMovies(from, to, null, 10);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).movieName()).isEqualTo("Movie A");
        }
    }
}
