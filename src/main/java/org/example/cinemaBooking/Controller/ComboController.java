package org.example.cinemaBooking.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.cinemaBooking.DTO.Request.Combo.CreateComboRequest;
import org.example.cinemaBooking.DTO.Request.Combo.UpdateComboRequest;
import org.example.cinemaBooking.DTO.Response.Combo.ComboResponse;
import org.example.cinemaBooking.Service.Product.ComboService;
import org.example.cinemaBooking.Shared.constant.ApiPaths;
import org.example.cinemaBooking.Shared.response.ApiResponse;
import org.example.cinemaBooking.Shared.response.PageResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RequiredArgsConstructor
@RequestMapping(ApiPaths.API_V1 + ApiPaths.Combo.BASE)
@RestController
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Combo", description = "quản lý combo (combo thức ăn/đồ uống)")
public class ComboController {
    ComboService comboService;

    @Operation(summary = "tạo combo",
            description = "taọ 1 comnbo mới cho ngươi dùng, yêu cầu ADMIN")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ApiResponse<ComboResponse> createCombo(@RequestBody @Valid CreateComboRequest request) {
        var response = comboService.createCombo(request);
        log.info("[COMBO_CONTROLLER]_REST request to create combo: {}", request.name());
        return ApiResponse.<ComboResponse>builder()
                .success(true)
                .message("combo created")
                .data(response)
                .build();
    }
    @Operation(summary = "cập nhật combo",
            description = "cập nhật 1 comnbo mới cho ngươi dùng, yêu cầu ADMIN")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ApiResponse<ComboResponse> updateCombo(@PathVariable String id, @RequestBody @Valid UpdateComboRequest request) {
        var response = comboService.updateCombo(id, request);
        log.info("[COMBO_CONTROLLER]_REST request to update combo: {}", id);
        return ApiResponse.<ComboResponse>builder()
                .success(true)
                .message("combo updated")
                .data(response)
                .build();
    }

    @Operation(summary = "xóa combo",
            description = "xóa 1 comnbo mới cho ngươi dùng, yêu cầu ADMIN")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteCombo(@PathVariable String id) {
        comboService.deleteCombo(id);
        log.info("[COMBO_CONTROLLER]_REST request to delete combo: {}", id);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("combo deleted")
                .build();
    }

    @Operation(summary = "lấy combo đang hoạt động cho nguoi dùng",
            description = "lấy tất cả combo đang hoạt động, có phân trang và sắp xếp", security = {})
    @GetMapping("/active")
    public ApiResponse<PageResponse<ComboResponse>> getActiveCombos(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(defaultValue = "price") String sortBy
    ) {
        var response = comboService.getAllCombosActive(page, size, sortBy, direction);
        log.info("[COMBO_CONTROLLER]_REST request to get active combos, page: {}, size: {}, sortBy: {}, direction: {}", page, size, sortBy, direction);
        return ApiResponse.<PageResponse<ComboResponse>>builder()
                .success(true)
                .message("pages found")
                .data(response)
                .build();
    }
    @Operation(summary = "lấy combo theo id",
            description = "lấy combo theo id")
    @GetMapping("/{id}")
    public ApiResponse<ComboResponse> getComboById(@PathVariable String id) {
        var response = comboService.getComboById(id);
        log.info("[COMBO_CONTROLLER]_REST request to get combo by id: {}", id);
        return ApiResponse.<ComboResponse>builder()
                .success(true)
                .message("combo found")
                .data(response)
                .build();
    }

    @Operation(summary = "lấy tất cả combo cho ADMIN",
            description = "lấy tất cả combo, có phân trang và sắp xếp, hỗ trợ tìm kiếm theo tên. Yêu cầu ADMIN")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ApiResponse<PageResponse<ComboResponse>> getCombos(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(defaultValue = "createdAt") String sortBy
    ) {
        return ApiResponse.<PageResponse<ComboResponse>>builder()
                .success(true)
                .message("pages found")
                .data(comboService.getCombos(page, size, keyword, direction, sortBy))
                .build();
    }



    @Operation(summary = "chuyển trạng thái hoạt động của combo",
            description = "chuyển trạng thái hoạt động của combo (active/inactive) theo ID. Yêu cầu ADMIN")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{comboId}/toggle-active")
    public ApiResponse<Void> toggleActive(@PathVariable String comboId) {
        comboService.toggleActiveCombo(comboId);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("combo toggled active")
                .build();
    }
}
