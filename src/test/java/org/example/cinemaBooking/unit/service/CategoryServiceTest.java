package org.example.cinemaBooking.unit.service;

import org.example.cinemaBooking.DTO.Request.CategoryRequest;
import org.example.cinemaBooking.DTO.Response.CategoryResponse;
import org.example.cinemaBooking.Entity.Category;
import org.example.cinemaBooking.Exception.AppException;
import org.example.cinemaBooking.Exception.ErrorCode;
import org.example.cinemaBooking.Mapper.CategoryMapper;
import org.example.cinemaBooking.Repository.CategoryRepository;
import org.example.cinemaBooking.Service.Movie.CategoryService;
import org.example.cinemaBooking.Shared.response.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryService categoryService;

    private Category category;
    private CategoryRequest categoryRequest;

    @BeforeEach
    void setUp() {
        category = new Category();
        category.setId("cat-001");
        category.setName("Action");

        categoryRequest = new CategoryRequest("Action");
    }

    @Nested
    class CreateTests {
        @Test
        void createCategory_Success() {
            // Given
            when(categoryRepository.existsByName("Action")).thenReturn(false);
            when(categoryMapper.toEntity(any())).thenReturn(category);
            when(categoryRepository.save(any())).thenReturn(category);
            when(categoryMapper.toResponse(any())).thenReturn(mock(CategoryResponse.class));

            // When
            categoryService.create(categoryRequest);

            // Then
            verify(categoryRepository).save(any());
        }

        @Test
        void createCategory_AlreadyExists_ThrowsException() {
            // Given
            when(categoryRepository.existsByName("Action")).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> categoryService.create(categoryRequest))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CATEGORY_ALREADY_EXISTS);
        }
    }

    @Nested
    class GetTests {
        @Test
        void getAll_Success() {
            // Given
            Page<Category> categoryPage = new PageImpl<>(Collections.singletonList(category));
            when(categoryRepository.findAll(any(Pageable.class))).thenReturn(categoryPage);
            when(categoryMapper.toResponse(any())).thenReturn(mock(CategoryResponse.class));

            // When
            PageResponse<CategoryResponse> result = categoryService.getAll(1, 10, null);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getItems()).hasSize(1);
            verify(categoryRepository).findAll(any(Pageable.class));
        }

        @Test
        void getAll_WithKeyword_Success() {
            // Given
            Page<Category> categoryPage = new PageImpl<>(Collections.singletonList(category));
            when(categoryRepository.findByNameContainingIgnoreCase(eq("Action"), any(Pageable.class))).thenReturn(categoryPage);
            when(categoryMapper.toResponse(any())).thenReturn(mock(CategoryResponse.class));

            // When
            PageResponse<CategoryResponse> result = categoryService.getAll(1, 10, "Action");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getItems()).hasSize(1);
            verify(categoryRepository).findByNameContainingIgnoreCase(eq("Action"), any(Pageable.class));
        }

        @Test
        void getById_Success() {
            // Given
            when(categoryRepository.findById("cat-001")).thenReturn(Optional.of(category));
            when(categoryMapper.toResponse(any())).thenReturn(mock(CategoryResponse.class));

            // When
            CategoryResponse result = categoryService.getById("cat-001");

            // Then
            assertThat(result).isNotNull();
            verify(categoryRepository).findById("cat-001");
        }

        @Test
        void getById_NotFound_ThrowsException() {
            // Given
            when(categoryRepository.findById("invalid")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> categoryService.getById("invalid"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CATEGORY_NOT_FOUND);
        }
    }

    @Nested
    class UpdateDeleteTests {
        @Test
        void updateCategory_Success() {
            // Given
            CategoryRequest updateRequest = new CategoryRequest("Sci-Fi");
            when(categoryRepository.findById("cat-001")).thenReturn(Optional.of(category));
            when(categoryRepository.existsByName("Sci-Fi")).thenReturn(false);
            when(categoryRepository.save(any())).thenReturn(category);
            when(categoryMapper.toResponse(any())).thenReturn(mock(CategoryResponse.class));

            // When
            categoryService.update("cat-001", updateRequest);

            // Then
            assertThat(category.getName()).isEqualTo("Sci-Fi");
            verify(categoryRepository).save(category);
        }

        @Test
        void updateCategory_NotFound_ThrowsException() {
            when(categoryRepository.findById("invalid")).thenReturn(Optional.empty());
            CategoryRequest updateRequest = new CategoryRequest("Sci-Fi");

            assertThatThrownBy(() -> categoryService.update("invalid", updateRequest))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CATEGORY_NOT_FOUND);
        }

        @Test
        void updateCategory_AlreadyExists_ThrowsException() {
            when(categoryRepository.findById("cat-001")).thenReturn(Optional.of(category));
            when(categoryRepository.existsByName("Sci-Fi")).thenReturn(true);
            CategoryRequest updateRequest = new CategoryRequest("Sci-Fi");

            assertThatThrownBy(() -> categoryService.update("cat-001", updateRequest))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CATEGORY_ALREADY_EXISTS);
        }

        @Test
        void delete_Success() {
            // Given
            when(categoryRepository.findById("cat-001")).thenReturn(Optional.of(category));

            // When
            categoryService.delete("cat-001");

            // Then
            verify(categoryRepository).deleteCategoryRelations("cat-001");
            verify(categoryRepository).delete(category);
        }

        @Test
        void delete_NotFound_ThrowsException() {
            when(categoryRepository.findById("invalid")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.delete("invalid"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CATEGORY_NOT_FOUND);
        }
    }
}
