package org.example.cinemaBooking.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cinemaBooking.DTO.Response.Statistics.DashboardSummaryResponse;
import org.example.cinemaBooking.DTO.Response.Statistics.RevenueSeriesItem;
import org.example.cinemaBooking.Service.Statistics.DashboardService;
import org.example.cinemaBooking.Shared.constant.ApiPaths;
import org.example.cinemaBooking.Shared.response.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping(ApiPaths.API_V1 + ApiPaths.Dashboard.BASE)
@Tag(name = "Dashboard", description = "API cho trang dashboard admin")
public class DashboardController {
        DashboardService dashboardService;

        @Operation(summary = "Lấy tổng quan dashboard", description = "Lấy tổng quan về doanh thu, số lượng vé bán ra, số lượng khách hàng, và số lượng suất chiếu của hôm nay")
        @SecurityRequirement(name = "bearerAuth")
        @PreAuthorize("hasRole('ADMIN')")
        @GetMapping(ApiPaths.Dashboard.SUMMARY)
        public ApiResponse<DashboardSummaryResponse> getDashboardSummary() {
                log.info("[DashboardController] getDashboardSummary called");
                DashboardSummaryResponse response = dashboardService.getSummary();
                return ApiResponse.<DashboardSummaryResponse>builder()
                                .message("Dashboard summary retrieved successfully")
                                .success(true)
                                .data(response)
                                .build();

        }

        @Operation(summary = "Lấy biểu đồ doanh thu 7 ngày", description = "Lấy dữ liệu doanh thu của 7 ngày gần nhất để hiển thị biểu đồ trên dashboard")
        @SecurityRequirement(name = "bearerAuth")
        @PreAuthorize("hasRole('ADMIN')")
        @GetMapping(ApiPaths.Dashboard.REVENUE_CHART)
        public ApiResponse<List<RevenueSeriesItem>> getRevenue7Days() {
                log.info("[DashboardController] getRevenue7Days called");
                var response = dashboardService.getRevenue7Days();
                return ApiResponse.<List<RevenueSeriesItem>>builder()
                                .message("Revenue for the last 7 days retrieved successfully")
                                .success(true)
                                .data(response)
                                .build();
        }
}
