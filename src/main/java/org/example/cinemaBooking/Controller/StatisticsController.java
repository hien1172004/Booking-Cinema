package org.example.cinemaBooking.Controller;

import com.cloudinary.Api;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.cinemaBooking.DTO.Response.Statistics.*;
import org.example.cinemaBooking.Service.Statistics.StatisticsService;
import org.example.cinemaBooking.Shared.constant.ApiPaths;
import org.example.cinemaBooking.Shared.response.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Level;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping(ApiPaths.API_V1 + ApiPaths.Dashboard.BASE + ApiPaths.Statistic.BASE)
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Tag(name = "Statistics", description = "API cho thống kê doanh thu, vé bán ra, doanh thu, số lượng vé, top phim theo bộ lọc theo")
public class StatisticsController {
    StatisticsService statisticsService;

    @Operation(summary = "Lấy tổng quan thống kê",
            description = "Lấy tổng quan về doanh thu, số lượng vé bán ra theo boi lọc theo ngày, tháng, năm, hoặc theo rạp")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(ApiPaths.Statistic.SUMMARY)
    public ApiResponse<DashboardSummaryResponse> getStatisticsSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String cinemaId,
            @RequestParam(required = false) String movieId
    ) {
        log.info("[StatisticsController] getStatisticsSummary called");
        DashboardSummaryResponse response = statisticsService.getSummary(from, to, cinemaId, movieId);
        return ApiResponse.<DashboardSummaryResponse>builder()
                .message("Statistics summary retrieved successfully")
                .success(true)
                .data(response)
                .build();
    }

    @Operation(summary = "Lấy biểu đồ doanh thu",
            description = "Lấy dữ liệu doanh thu theo bộ lọc theo ngày, tháng, năm, hoặc theo rạp để hiển thị biểu đồ ")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(ApiPaths.Statistic.REVENUE_CHART)
    ApiResponse<List<RevenueSeriesItem>> getRevenueChart(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String cinemaId
    ){
        var response = statisticsService.getRevenueChart(from, to, cinemaId);
        log.info("[StatisticsController] getRevenueChart called");
        return ApiResponse.<List<RevenueSeriesItem>>builder()
                .message("Revenue chart data retrieved successfully")
                .success(true)
                .data(response)
                .build();
    }


    @Operation(summary = "Lấy biểu đồ vé bán ra",
            description = "Lấy dữ liệu số lượng vé bán ra theo bộ lọc theo ngày, tháng, năm, hoặc theo rạp để hiển thị biểu đồ ")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(ApiPaths.Statistic.TICKET_CHART)
    ApiResponse<List<TicketSeriesItem>> getTicketsSoldChart(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String cinemaId,
            @RequestParam(required = false) String movieId
    ){
        var response = statisticsService.getTicketChart(from, to, cinemaId, movieId);
        log.info("[StatisticsController] getTicketsSoldChart called");
        return ApiResponse.<List<TicketSeriesItem>>builder()
                .message("Tickets sold chart data retrieved successfully")
                .success(true)
                .data(response)
                .build();
    }

    @Operation(summary = "Lấy top phim",
            description = "Lấy danh sách top phim theo số lượng vé bán ra theo bộ lọc theo ngày, tháng, năm, hoặc theo rạp")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(ApiPaths.Statistic.TOP_MOVIES)
    ApiResponse<List<TopMovieResponse>> getTopMovies(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String cinemaId,
            @RequestParam(defaultValue = "10") int limit
    ) {
        var response = statisticsService.getTopMovies(from, to, cinemaId, limit);
        log.info("[StatisticsController] getTopMovies called");
        return ApiResponse.<List<TopMovieResponse>>builder()
                .message("Top movies retrieved successfully")
                .success(true)
                .data(response)
                .build();
    }
}
