package org.example.cinemaBooking.Service.Statistics;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.cinemaBooking.DTO.Response.Statistics.*;
import org.example.cinemaBooking.Repository.PaymentRepository;
import org.example.cinemaBooking.Repository.TicketRepository;
import org.example.cinemaBooking.Shared.enums.PaymentStatus;
import org.example.cinemaBooking.Shared.enums.TicketStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class StatisticsService {

    PaymentRepository paymentRepository;
    TicketRepository ticketRepository;

    public DashboardSummaryResponse getSummary(LocalDate from, LocalDate to, String cinemaId, String movieId) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();

        BigDecimal revenue = paymentRepository.getRevenue(
                PaymentStatus.SUCCESS,
                start,
                end,
                cinemaId);
        int tickets = ticketRepository.countTickets(
                TicketStatus.VALID,
                start,
                end,
                cinemaId,
                movieId);
        int bookings = ticketRepository.countBookings(
                TicketStatus.VALID,
                start,
                end,
                cinemaId,
                movieId);
        int movies = ticketRepository.countMovies(
                TicketStatus.VALID,
                start,
                end,
                cinemaId,
                movieId);


        return new DashboardSummaryResponse(revenue, tickets, null, null, bookings, movies);
    }

    public List<RevenueSeriesItem>getRevenueChart(LocalDate from, LocalDate to, String cinemaId) {
        List<RevenueSeriesItem> raw = paymentRepository.getRevenueSeries(
                PaymentStatus.SUCCESS,
                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay(),
                cinemaId);
        return fillMissingRevenueDates(raw, from, to);
    }

    public List<TicketSeriesItem> getTicketChart(LocalDate from, LocalDate to, String cinemaId, String movieId) {
        List<TicketSeriesItem> raw = ticketRepository.getTicketSeries(
                TicketStatus.VALID,
                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay(),
                cinemaId, movieId);
        return fillMissingTicketDates(raw, from, to);
    }

    public List<TopMovieResponse> getTopMovies(LocalDate from, LocalDate to, String cinemaId, int limit) {
        limit = Math.min(limit <= 0 ? 10 : limit, 20);
        List<TopMovieResponse> responses = ticketRepository.getTopMovies(
                PaymentStatus.SUCCESS,
                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay(),
                cinemaId,
                PageRequest.of(0, limit));
        return responses.stream().map(item ->
                new TopMovieResponse(item.movieName(),
                        item.imageUrl(),
                        item.tickets() != null ? item.tickets() : 0,
                        item.revenue() != null ? item.revenue() : BigDecimal.ZERO)).toList();
    }

    private List<RevenueSeriesItem> fillMissingRevenueDates(List<RevenueSeriesItem> raw, LocalDate from, LocalDate to) {
        Map<LocalDate, BigDecimal> map = raw.stream()
                .collect(Collectors.toMap(
                        RevenueSeriesItem::getDate,
                        RevenueSeriesItem::getValue));
        List<RevenueSeriesItem> result = new ArrayList<>();
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        for (int i = 0; i < days; i++) {
            LocalDate date = from.plusDays(i);
            result.add(new RevenueSeriesItem(date, map.getOrDefault(date, BigDecimal.ZERO)));
        }
        return result;
    }

    private List<TicketSeriesItem> fillMissingTicketDates(List<TicketSeriesItem> raw, LocalDate from, LocalDate to) {
        Map<LocalDate, Long> map = raw.stream()
                .collect(Collectors.toMap(
                        TicketSeriesItem::getDate,
                        TicketSeriesItem::getCount));
        List<TicketSeriesItem> result = new ArrayList<>();
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        for (int i = 0; i < days; i++) {
            LocalDate date = from.plusDays(i);
            result.add(new TicketSeriesItem(date, map.getOrDefault(date, 0L)));
        }
        return result;
    }
}
