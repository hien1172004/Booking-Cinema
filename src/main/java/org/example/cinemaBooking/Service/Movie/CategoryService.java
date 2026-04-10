package org.example.cinemaBooking.Service.Movie;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.cinemaBooking.DTO.Request.CategoryRequest;
import org.example.cinemaBooking.DTO.Response.CategoryResponse;
import org.example.cinemaBooking.Entity.Category;
import org.example.cinemaBooking.Exception.AppException;
import org.example.cinemaBooking.Exception.ErrorCode;
import org.example.cinemaBooking.Mapper.CategoryMapper;
import org.example.cinemaBooking.Repository.CategoryRepository;
import org.example.cinemaBooking.Shared.response.PageResponse;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CategoryService {

    CategoryRepository categoryRepository;
    CategoryMapper categoryMapper;

    /**
     * Tạo mới một thể loại phim.
     * <p>Xoá bộ nhớ đệm danh sách thể loại sau khi tạo thành công.</p>
     *
     * @param request Thông tin thể loại cần tạo.
     * @return CategoryResponse thông tin thể loại tạo mới.
     */
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByName(request.name())) {
            throw new AppException(ErrorCode.CATEGORY_ALREADY_EXISTS);
        }

        Category category = categoryMapper.toEntity(request);

        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    /**
     * Lấy danh sách thể loại phim (hỗ trợ phân trang và tìm kiếm).
     * <p>Danh sách kết quả được lưu vào cache "categories" theo tham số truy vấn.</p>
     *
     * @param page Số thứ tự trang
     * @param size Số lượng bản ghi trên một trang
     * @param key Từ khoá tìm kiếm theo tên thể loại
     * @return PageResponse thông tin danh sách đã phân trang
     */
    @Cacheable(value = "categories", key = "#page + '-' + #size + '-' + (#key ?: '')")
    public PageResponse<CategoryResponse> getAll(int page, int size, String key) {
        int pageNumber = Math.max(page - 1, 0);

        Pageable pageable = PageRequest.of(pageNumber, size, Sort.by("name").ascending());

        Page<Category> categoryPage = (key != null && !key.isBlank())
                ? categoryRepository.findByNameContainingIgnoreCase(key, pageable)
                : categoryRepository.findAll(pageable);

        List<CategoryResponse> responses = categoryPage.getContent().stream()
                .map(categoryMapper::toResponse)
                .toList();

        return PageResponse.<CategoryResponse>builder()
                .page(page)
                .size(size)
                .totalElements(categoryPage.getTotalElements())
                .totalPages(categoryPage.getTotalPages())
                .items(responses)
                .build();
    }

    /**
     * Lấy thông tin chi tiết của một thể loại.
     * <p>Kết quả được lưu vào cache "category" với key là ID của thể loại.</p>
     *
     * @param id ID của thể loại
     * @return CategoryResponse thông tin chi tiết
     */
    @Cacheable(value = "category", key = "#id")
    public CategoryResponse getById(String id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        return categoryMapper.toResponse(category);
    }

    /**
     * Cập nhật thông tin của một thể loại phim.
     * <p>Xoá bộ đệm của danh sách thể loại và bộ đệm của thể loại tương ứng.</p>
     *
     * @param id ID của thể loại cần cập nhật
     * @param request Thông tin cập nhật
     * @return CategoryResponse thông tin sau cập nhật
     */
    @Caching(evict = {
            @CacheEvict(value = "categories", allEntries = true),
            @CacheEvict(value = "category", key = "#id")
    })
    public CategoryResponse update(String id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        if (!category.getName().equals(request.name()) &&
                categoryRepository.existsByName(request.name())) {
            throw new AppException(ErrorCode.CATEGORY_ALREADY_EXISTS);
        }

        category.setName(request.name());

        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    /**
     * Xoá cứng (Hard delete) một thể loại phim khỏi hệ thống.
     * <p>Đồng thời xoá toàn bộ cache liên quan tới thể loại này.</p>
     *
     * @param id ID của thể loại cần xoá
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "categories", allEntries = true),
            @CacheEvict(value = "category", key = "#id")
    })
    public void delete(String id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        // Xóa quan hệ Many-to-Many trong bảng join (native query)
        categoryRepository.deleteCategoryRelations(id);

        categoryRepository.delete(category);
    }
}