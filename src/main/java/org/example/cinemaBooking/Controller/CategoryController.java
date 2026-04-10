package org.example.cinemaBooking.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.cinemaBooking.DTO.Request.CategoryRequest;
import org.example.cinemaBooking.DTO.Response.CategoryResponse;
import org.example.cinemaBooking.Service.Movie.CategoryService;
import org.example.cinemaBooking.Shared.constant.ApiPaths;
import org.example.cinemaBooking.Shared.response.ApiResponse;
import org.example.cinemaBooking.Shared.response.PageResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Category", description = "quản lý thể loại phim")
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@RequestMapping(ApiPaths.API_V1 + ApiPaths.Category.BASE)
public class CategoryController {
    CategoryService categoryService;

    @Operation(summary = "Thêm thể loại mới",
            description = "Tạo thể loại phim mới. Yêu cầu quyền ADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ApiResponse<CategoryResponse> addCategory(@RequestBody @Valid CategoryRequest categoryRequest) {
        var categoryResponse = categoryService.create(categoryRequest);
        log.info("[CATEGORY CONTROLLER] Category {} has been created", categoryResponse.id());
        return ApiResponse.<CategoryResponse>builder()
                .success(true)
                .data(categoryResponse)
                .build();
    }

    @Operation(summary = "Cập nhật thể loại",
            description = "Cập nhật thể loại phim theo ID. Yêu cầu quyền ADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ApiResponse<CategoryResponse> updateCategory(@PathVariable String id,
                                                        @RequestBody @Valid CategoryRequest categoryRequest) {
        var categoryResponse = categoryService.update(id, categoryRequest);
        log.info("[CATEGORY CONTROLLER] Category {} has been updated", categoryResponse.id());
        return ApiResponse.<CategoryResponse>builder()
                .success(true)
                .data(categoryResponse)
                .build();
    }

    @Operation(summary = "Xóa thể loại",
            description = "Xóa thể loại phim theo ID. Yêu cầu quyền ADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteCategory(@PathVariable String id) {
        categoryService.delete(id);
        log.info("[CATEGORY CONTROLLER] Category {} has been deleted", id);
        return ApiResponse.<Void>builder()
                .success(true)                .build();
    }

    @Operation(summary = "Lấy chi tiết thể loại",
            description = "Lấy chi tiết thể loại phim theo ID.",
            security = {})
    @GetMapping("/{id}")
    ApiResponse<CategoryResponse> getCategory(@PathVariable String id) {
        var categoryResponse = categoryService.getById(id);
        return ApiResponse.<CategoryResponse>builder()
                .success(true)
                .data(categoryResponse)
                .build();
    }

    @Operation(summary = "Lấy tất cả thể loại",
            description = "Lấy danh sách phân trang các thể loại phim. Hỗ trợ tìm kiếm theo tên.",
            security = {})
    @GetMapping
    ApiResponse<PageResponse<CategoryResponse>> getAllCategories(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String key
    ) {
        var categoryPage = categoryService.getAll(page, size, key);
        return ApiResponse.<PageResponse<CategoryResponse>>builder()
                .success(true)
                .data(categoryPage)
                .build();
    }

}
