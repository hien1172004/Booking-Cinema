package org.example.cinemaBooking.Service.Product;

import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.cinemaBooking.DTO.Request.Product.CreateProductRequest;
import org.example.cinemaBooking.DTO.Request.Product.UpdateProductRequest;
import org.example.cinemaBooking.DTO.Response.Product.ProductResponse;
import org.example.cinemaBooking.Entity.Product;
import org.example.cinemaBooking.Exception.AppException;
import org.example.cinemaBooking.Exception.ErrorCode;
import org.example.cinemaBooking.Mapper.ProductMapper;
import org.example.cinemaBooking.Repository.ProductRepository;
import org.example.cinemaBooking.Shared.response.PageResponse;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductService {
    ProductRepository productRepository;
    ProductMapper productMapper;

    /**
     * Tạo mới một sản phẩm (đồ ăn, thức uống).
     * <p>Xoá bộ đệm các danh sách sản phẩm để cập nhật dữ liệu mới.</p>
     *
     * @param request Dữ liệu sản phẩm mới
     * @return ProductResponse Thông tin sản phẩm vừa tạo
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "products-active", allEntries = true)
    })
    public ProductResponse createProduct(CreateProductRequest request){
        Optional<Product> existingProduct = productRepository.findByName(request.name());

        if(existingProduct.isPresent()){
           Product product = existingProduct.get();
           if(product.isDeleted()){
               product.restore();
               product.setImage(request.image());
               product.setActive(true);
               product.setPrice(request.price());
               Product savedProduct = productRepository.save(product);
               return productMapper.toResponse(savedProduct);
           }
           throw new AppException(ErrorCode.PRODUCT_ALREADY_EXISTS);
        }

        Product product = productMapper.toEntity(request);
        Product savedProduct = productRepository.save(product);
        return productMapper.toResponse(savedProduct);
    }

    /**
     * Cập nhật thông tin sản phẩm.
     * <p>Xoá bộ đệm danh sách sản phẩm và chi tiết sản phẩm đó.</p>
     *
     * @param id      ID của sản phẩm cần cập nhật
     * @param request Thông tin cập nhật
     * @return ProductResponse Thông tin sản phẩm sau khi cập nhật
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "products-active", allEntries = true),
            @CacheEvict(value = "product", key = "#id")
    })
    public ProductResponse updateProduct(String id, UpdateProductRequest request){
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if(request.name() != null && !request.name().isEmpty()){
            Optional<Product> existingProduct = productRepository.findByName(request.name());
            if(existingProduct.isPresent() && !existingProduct.get().getId().equals(id)){
                throw new AppException(ErrorCode.PRODUCT_ALREADY_EXISTS);
            }
            product.setName(request.name());
        }

        productMapper.update(product, request);

        return productMapper.toResponse(product);
    }

    /**
     * Xoá mềm một sản phẩm.
     * <p>Xoá bộ đệm danh sách sản phẩm và chi tiết sản phẩm đó.</p>
     *
     * @param id ID của sản phẩm cần xoá
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "products-active", allEntries = true),
            @CacheEvict(value = "product", key = "#id")
    })
    public void deleteProduct(String id){
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        product.softDelete();
        product.setActive(false);
        productRepository.save(product);
    }

    /**
     * Lấy thông tin chi tiết một sản phẩm.
     * <p>Kết quả được lưu vào cache "product".</p>
     *
     * @param id ID của sản phẩm
     * @return ProductResponse Thông tin chi tiết sản phẩm
     */
    @Cacheable(value = "product", key = "#id")
    public ProductResponse getProductById(String id){
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        return productMapper.toResponse(product);
    }

    /**
     * Bật/Tắt trạng thái hoạt động của sản phẩm.
     * <p>Xoá bộ đệm danh sách sản phẩm và chi tiết sản phẩm đó.</p>
     *
     * @param id ID của sản phẩm
     */
    @Caching(evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "products-active", allEntries = true),
            @CacheEvict(value = "product", key = "#id")
    })
    public void toggleActiveProduct(String id){
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        product.setActive(!product.getActive());
        productRepository.save(product);
    }

    /**
     * Lấy danh sách tất cả các sản phẩm (hỗ trợ phân trang và tìm kiếm).
     * <p>Kết quả phân trang được lưu vào cache "products".</p>
     *
     * @param page      Số thứ tự trang
     * @param size      Kích thước trang
     * @param sortBy    Cột cần sắp xếp
     * @param direction Hướng sắp xếp (asc/desc)
     * @param keyword   Từ khoá tìm kiếm
     * @return PageResponse Danh sách sản phẩm (tất cả trạng thái)
     */
    @Cacheable(value = "products", key = "#page + '-' + #size + '-' + #sortBy + '-' + #direction + '-' + (#keyword ?: '')")
    public PageResponse<ProductResponse> getAllProducts(
            int page,
            int size,
            String sortBy,
            String direction,
            String keyword
    ) {
        int pageNumber = page > 0 ? page -1 : 0;

        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNumber, size, sort);

        Page<Product> productPage;

        if (keyword != null && !keyword.isBlank()) {
            productPage = productRepository
                    .findByNameContainingIgnoreCaseAndDeletedFalse(keyword, pageable);
        } else {
            productPage = productRepository.findAllByDeletedFalse(pageable);
        }

        return PageResponse.<ProductResponse>builder()
                .page(page)
                .size(size)
                .totalElements(productPage.getTotalElements())
                .totalPages(productPage.getTotalPages())
                .items(productPage.getContent()
                        .stream()
                        .map(productMapper::toResponse)
                        .toList())
                .build();
    }

    /**
     * Lấy danh sách các sản phẩm đang hoạt động (Active).
     * <p>Kết quả phân trang được lưu vào cache "products-active".</p>
     *
     * @param page      Số thứ tự trang
     * @param size      Kích thước trang
     * @param sortBy    Cột cần sắp xếp
     * @param direction Hướng sắp xếp (asc/desc)
     * @return PageResponse Danh sách sản phẩm Active
     */
    @Cacheable(value = "products-active", key = "#page + '-' + #size + '-' + #sortBy + '-' + #direction")
    public PageResponse<ProductResponse> getProductActive(
            int page,
            int size,
            String sortBy,
            String direction) {

        int pageNumber = page > 0 ? page -1 : 0;
        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(pageNumber, size, Sort.by(sortDirection, sortBy));
        Page<Product> productPage = productRepository.findByActiveTrueAndDeletedFalse(pageable);

        List<ProductResponse> productResponses = productPage.getContent()
                .stream()
                .map(productMapper::toResponse)
                .toList();

        return PageResponse.<ProductResponse>builder()
                .page(page)
                .size(size)
                .totalElements(productPage.getTotalElements())
                .totalPages(productPage.getTotalPages())
                .items(productResponses)
                .build();
    }
}
