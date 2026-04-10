package org.example.cinemaBooking.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.cinemaBooking.DTO.Request.Promotion.CreatePromotionRequest;
import org.example.cinemaBooking.DTO.Request.Promotion.PromotionFilterRequest;
import org.example.cinemaBooking.DTO.Request.Promotion.UpdatePromotionRequest;
import org.example.cinemaBooking.DTO.Response.Promotion.PromotionResponse;
import org.example.cinemaBooking.DTO.Response.ValidationResultResponse;
import org.example.cinemaBooking.Service.Promotion.PromotionService;
import org.example.cinemaBooking.Shared.constant.ApiPaths;
import org.example.cinemaBooking.Shared.response.ApiResponse;
import org.example.cinemaBooking.Shared.response.PageResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping(ApiPaths.API_V1 + ApiPaths.Promotion.BASE)
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Promotion", description = "quản lý khuyến mãi")
public class PromotionController {

    PromotionService promotionService;

    @Operation(summary = "Tạo khuyến mãi mới",
            description = "Tạo một khuyến mãi mới. Yêu cầu quyền ADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ApiResponse<PromotionResponse> create(@RequestBody @Valid CreatePromotionRequest request) {
        return ApiResponse.<PromotionResponse>builder()
                .success(true)
                .message("Promotion created successfully")
                .data(promotionService.createPromotion(request))
                .build();
    }

    @Operation(summary = "Cập nhật khuyến mãi",
            description = "Cập nhật thông tin khuyến mãi theo ID. Yêu cầu quyền ADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ApiResponse<PromotionResponse> update(@PathVariable String id,
                                                 @RequestBody UpdatePromotionRequest request) {
        return ApiResponse.<PromotionResponse>builder()
                .success(true)
                .message("Promotion updated successfully")
                .data(promotionService.updatePromotion(id, request))
                .build();
    }

    @Operation(summary = "Xóa khuyến mãi",
            description = "Xóa khuyến mãi theo ID. Yêu cầu quyền ADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        promotionService.deletePromotion(id);

        return ApiResponse.<Void>builder()
                .success(true)
                .message("Promotion deleted successfully")
                .build();
    }

    @Operation(summary = "Lấy khuyến mãi đang hoạt động cho nguoi dung",
            description = "Lấy danh sách khuyến mãi đang hoạt động. Không yêu cầu xác thực.")
    @GetMapping("/active")
    public ApiResponse<PageResponse<PromotionResponse>> getActivePromotion(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        return ApiResponse.<PageResponse<PromotionResponse>>builder()
                .success(true)
                .data(promotionService.getActivePromotions(page, size, sortBy, sortDir))
                .build();
    }

    @Operation(summary = "Lấy chi tiết khuyến mãi",
            description = "Lấy chi tiết khuyến mãi theo ID.")
    @GetMapping("/{id}")
    public ApiResponse<PromotionResponse> getById(@PathVariable String id) {
        return ApiResponse.<PromotionResponse>builder()
                .success(true)
                .data(promotionService.getPromotionById(id))
                .build();
    }

    @Operation(summary = "Lấy chi tiết khuyến mãi theo code",
            description = "Lấy chi tiết khuyến mãi theo code.")
    @GetMapping("/code/{code}")
    public ApiResponse<PromotionResponse> getByCode(@PathVariable String code) {
        return ApiResponse.<PromotionResponse>builder()
                .success(true)
                .data(promotionService.getPromotionByCode(code))
                .build();
    }


    @Operation(summary = "Lấy danh sách khuyến mãi cho ADMIN",
            description = "Lấy danh sách khuyến mãi, hỗ trợ phân trang, sắp xếp và tìm kiếm theo code, tên, ngày bắt đầu, ngày kết thúc, giá trị đơn hàng tối thiểu và tối đa. Yêu cầu quyền ADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ApiResponse<PageResponse<PromotionResponse>> getPromotions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) BigDecimal minOrderValue,
            @RequestParam(required = false) BigDecimal maxOrderValue) {
        PromotionFilterRequest filterRequest = new PromotionFilterRequest(
                code, name, startDate, endDate, minOrderValue, maxOrderValue
        );

        PageResponse<PromotionResponse> response = promotionService.getPromotions(
                page, size, sortBy, sortDir, filterRequest
        );

        return ApiResponse.<PageResponse<PromotionResponse>>builder()
                .success(true)
                .data(response)
                .build();
    }

    @Operation(summary = "Xem trước khuyến mãi",
            description = "Xem trước kết quả áp dụng khuyến mãi cho một đơn hàng cụ thể. Không yêu cầu xác thực.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/preview")
    public ApiResponse<ValidationResultResponse> preview(
            @RequestParam String code,
            @RequestParam String userId,
            @RequestParam BigDecimal orderValue
    ) {
        return ApiResponse.<ValidationResultResponse>builder()
                .success(true)
                .data(promotionService.previewPromotion(code, userId, orderValue))
                .build();
    }

    // APPLY
    @Operation(summary = "Áp dụng khuyến mãi",
            description = "Áp dụng khuyến mãi cho một đơn hàng cụ thể. Yêu cầu xác thực.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/apply")
    public ApiResponse<Void> apply(
            @RequestParam String promotionId,
            @RequestParam String userId
    ) {
        promotionService.applyPromotion(promotionId, userId);

        return ApiResponse.<Void>builder()
                .success(true)
                .message("Promotion applied successfully")
                .build();
    }
}