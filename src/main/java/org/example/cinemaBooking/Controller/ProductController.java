package org.example.cinemaBooking.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.cinemaBooking.DTO.Request.Product.CreateProductRequest;
import org.example.cinemaBooking.DTO.Request.Product.UpdateProductRequest;
import org.example.cinemaBooking.DTO.Response.Product.ProductResponse;
import org.example.cinemaBooking.Service.Product.ProductService;
import org.example.cinemaBooking.Shared.constant.ApiPaths;
import org.example.cinemaBooking.Shared.response.ApiResponse;
import org.example.cinemaBooking.Shared.response.PageResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping(ApiPaths.API_V1 + ApiPaths.Product.BASE)
@Tag(name = "Product", description = "quản lý sản phẩm (đồ ăn/đồ uống)")
public class ProductController {
    ProductService productService;

    @Operation(summary = "Tạo sản phẩm mới",
            description = "Tạo một sản phẩm mới. Yêu cầu quyền ADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ApiResponse<ProductResponse> createProduct(@RequestBody @Valid CreateProductRequest request){
        ProductResponse response = productService.createProduct(request);
        log.info(("[PRODUCT_CONTROLLER] Created product with id: {}"), response.id());
        return ApiResponse.<ProductResponse>builder()
                .success(true)
                .message("Product created successfully")
                .data(response)
                .build();
    }

    @Operation(summary = "Cập nhật sản phẩm",
            description = "Cập nhật thông tin sản phẩm theo ID. Yêu cầu quyền ADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ApiResponse<ProductResponse> updateProduct(@PathVariable String id, @RequestBody @Valid UpdateProductRequest request) {
        ProductResponse response = productService.updateProduct(id, request);
        log.info(("[PRODUCT_CONTROLLER] Updated product with id: {}"), response.id());
        return ApiResponse.<ProductResponse>builder()
                .success(true)
                .message("Product updated successfully")
                .data(response)
                .build();
    }

    @Operation(summary = "Xóa sản phẩm",
            description = "Xóa sản phẩm theo ID. Yêu cầu quyền ADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteProduct(@PathVariable String id) {
        productService.deleteProduct(id);
        log.info(("[PRODUCT_CONTROLLER] Deleted product with id: {}"), id);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Product deleted successfully")
                .build();
    }


    @Operation(summary = "Lấy chi tiết sản phẩm",
            description = "Lấy chi tiết sản phẩm theo ID.")
    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> getProductById(@PathVariable String id) {
        var productResponse = productService.getProductById(id);
        log.info(("[PRODUCT_CONTROLLER] Retrieved product with id: {}"), id);
        return ApiResponse.<ProductResponse>builder()
                .success(true)
                .message("Product retrieved successfully")
                .data(productResponse)
                .build();
    }

    @Operation(summary = "Chuyển trạng thái hoạt động của sản phẩm",
            description = "Chuyển trạng thái hoạt động của sản phẩm (active/inactive) theo ID. Yêu cầu quyền ADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/toggle-active")
    public ApiResponse<ProductResponse> toggleProductActiveStatus(@PathVariable String id) {
        productService.toggleActiveProduct(id);
        log.info("[PRODUCT_CONTROLLER] Toggled active status for product with id: {}", id);
        return ApiResponse.<ProductResponse>builder()
                .success(true)
                .message("Product active status toggled successfully")
                .data(productService.getProductById(id))
                .build();
    }

    @Operation(summary = "Lấy danh sách sản phẩm cho ADMIN",
            description = "Lấy danh sách sản phẩm, hỗ trợ phân trang, sắp xếp và tìm kiếm theo tên. Yêu cầu quyền ADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ApiResponse<PageResponse<ProductResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.<PageResponse<ProductResponse>>builder()
                .success(true)
                .message("Products retrieved successfully")
                .data(productService.getAllProducts(page, size, sortBy, direction, keyword))
                .build();
    }

    @Operation(summary = "Lấy danh sách sản phẩm đang hoạt động cho nguoi dung",
            description = "Lấy danh sách sản phẩm đang hoạt động, hỗ trợ phân trang và sắp xếp.")
    @GetMapping("/active")
    public ApiResponse<PageResponse<ProductResponse>> getActiveProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "price") String sortBy,
            @RequestParam(defaultValue = "asc") String direction){
        return ApiResponse.<PageResponse<ProductResponse>>builder()
                .success(true)
                .message("Active products retrieved successfully")
                .data(productService.getProductActive(page, size, sortBy, direction))
                .build();
    }
}
