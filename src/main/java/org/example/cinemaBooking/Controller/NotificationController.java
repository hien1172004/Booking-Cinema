package org.example.cinemaBooking.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.cinemaBooking.DTO.Response.Notification.NotificationResponse;
import org.example.cinemaBooking.Service.Notification.NotificationService;
import org.example.cinemaBooking.Shared.constant.ApiPaths;
import org.example.cinemaBooking.Shared.response.ApiResponse;
import org.example.cinemaBooking.Shared.response.PageResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Notification", description = "thông báo người dùng")
@RequestMapping((ApiPaths.API_V1 + ApiPaths.Notification.BASE))
public class NotificationController {
    NotificationService notificationService;

    @Operation(summary = "Lấy thông báo của tôi",
            description = "Lấy danh sách thông báo của người dùng đã đăng nhập, có phân trang. Yêu cầu người dùng phải xác thực.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResponse<NotificationResponse>> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.<PageResponse<NotificationResponse>>builder()
                .success(true)
                .message("Notifications retrieved successfully")
                .data(notificationService.getMyNotifications(page, size))
                .build();
    }

    @Operation(summary = "Đếm số lượng thông báo chưa đọc",
            description = "Đếm số lượng thông báo chưa đọc của người dùng. Yêu cầu người dùng phải xác thực.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Integer> countUnread() {
        return ApiResponse.<Integer>builder()
                .success(true)
                .message("Unread notifications count retrieved successfully")
                .data(notificationService.countUnread())
                .build();
    }

    @Operation(summary = "Đánh dấu tất cả thông báo là đã đọc",
            description = "Đánh dấu tất cả thông báo của người dùng là đã đọc. Yêu cầu người dùng phải xác thực.")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/mark-all-read")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> markAllAsRead() {
        notificationService.markAllAsRead();
        return ApiResponse.<Void>builder()
                .success(true)
                .message("All notifications marked as read successfully")
                .build();
    }

    @Operation(summary = "Đánh dấu một thông báo là đã đọc",
            description = "Đánh dấu một thông báo cụ thể là đã đọc theo ID. Yêu cầu người dùng phải xác thực.")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> markAsRead(@PathVariable String id) {
        notificationService.markAsRead(id);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Notification marked as read successfully")
                .build();
    }
    @Operation(summary = "Lấy chi tiết thông báo",
            description = "Lấy chi tiết một thông báo cụ thể theo ID. Yêu cầu người dùng phải xác thực.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<NotificationResponse> getNotificationById(@PathVariable String id) {
        return ApiResponse.<NotificationResponse>builder()
                .success(true)
                .message("Notification retrieved successfully")
                .data(notificationService.getNotificationById(id))
                .build();
    }
}
