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

    // CREATE
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByName(request.name())) {
            throw new AppException(ErrorCode.CATEGORY_ALREADY_EXISTS);
        }

        Category category = categoryMapper.toEntity(request);

        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    // GET ALL + SEARCH + PAGINATION
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

    // GET BY ID
    public CategoryResponse getById(String id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        return categoryMapper.toResponse(category);
    }

    // UPDATE
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

    // DELETE (HARD DELETE chuẩn)
    @Transactional
    public void delete(String id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        // Xóa quan hệ Many-to-Many trong bảng join (native query)
        categoryRepository.deleteCategoryRelations(id);

        categoryRepository.delete(category);
    }
}