package org.example.cinemaBooking.Service.Statistics;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.cinemaBooking.DTO.Response.Statistics.DashboardSummaryResponse;
import org.example.cinemaBooking.DTO.Response.Statistics.RevenueSeriesItem;
import org.example.cinemaBooking.DTO.Response.Statistics.TimeSeriesItem;
import org.example.cinemaBooking.DTO.Response.Statistics.TimeSeriesResponse;
import org.example.cinemaBooking.Repository.PaymentRepository;
import org.example.cinemaBooking.Repository.ShowtimeRepository;
import org.example.cinemaBooking.Repository.TicketRepository;
import org.example.cinemaBooking.Repository.UserRepository;
import org.example.cinemaBooking.Shared.enums.PaymentStatus;
import org.example.cinemaBooking.Shared.enums.TicketStatus;
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
public class DashboardService {

    PaymentRepository paymentRepository;
    TicketRepository ticketRepository;
    UserRepository userRepository;
    ShowtimeRepository showtimeRepository;

    public DashboardSummaryResponse getSummary() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        BigDecimal revenue = paymentRepository.getRevenue(PaymentStatus.SUCCESS, start, end, null);
        int tickets = ticketRepository.countTickets(List.of(TicketStatus.VALID, TicketStatus.USED, TicketStatus.EXPIRED), start, end, null, null);
        int users = userRepository.countUsers(start, end);
        int showtimes = showtimeRepository.countShowtimes(start, end);

        return new DashboardSummaryResponse(revenue, tickets, users, showtimes);
    }

    public List<RevenueSeriesItem> getRevenue7Days() {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(6);
        List<RevenueSeriesItem> raw = paymentRepository.getRevenueSeries(PaymentStatus.SUCCESS, from.atStartOfDay(), to.plusDays(1).atStartOfDay(), null);
        return fillMissingDates(raw, from, to);
    }

    private List<RevenueSeriesItem> fillMissingDates(List<RevenueSeriesItem> raw, LocalDate from, LocalDate to) {
        Map<LocalDate, BigDecimal> map = raw.stream().collect(Collectors.toMap(RevenueSeriesItem::getDate, RevenueSeriesItem::getValue));
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        List<RevenueSeriesItem> result = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate date = from.plusDays(i);
            result.add(new RevenueSeriesItem(date, map.getOrDefault(date, BigDecimal.ZERO)));
        }
        return result;
    }
}