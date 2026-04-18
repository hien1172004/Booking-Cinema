package org.example.cinemaBooking.unit.service;

import org.example.cinemaBooking.DTO.Request.Product.CreateProductRequest;
import org.example.cinemaBooking.DTO.Request.Product.UpdateProductRequest;
import org.example.cinemaBooking.DTO.Response.Product.ProductResponse;
import org.example.cinemaBooking.Entity.Product;
import org.example.cinemaBooking.Exception.AppException;
import org.example.cinemaBooking.Exception.ErrorCode;
import org.example.cinemaBooking.Mapper.ProductMapper;
import org.example.cinemaBooking.Repository.ProductRepository;
import org.example.cinemaBooking.Service.Product.ProductService;
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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductService productService;

    private Product product;
    private CreateProductRequest createRequest;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId("prod-001");
        product.setName("Popcorn");
        product.setPrice(BigDecimal.valueOf(50000));
        product.setActive(true);

        createRequest = new CreateProductRequest("Popcorn", BigDecimal.valueOf(50000), "Image");
    }

    @Nested
    class CRUDTests {
        @Test
        void createProduct_Success() {
            // Given
            when(productRepository.findByName("Popcorn")).thenReturn(Optional.empty());
            when(productMapper.toEntity(any())).thenReturn(product);
            when(productRepository.save(any())).thenReturn(product);
            when(productMapper.toResponse(any())).thenReturn(mock(ProductResponse.class));

            // When
            productService.createProduct(createRequest);

            // Then
            verify(productRepository).save(any());
        }

        @Test
        void createProduct_RestoreDeleted_Success() {
            // Given
            product.setDeleted(true);
            product.setActive(false);
            when(productRepository.findByName("Popcorn")).thenReturn(Optional.of(product));
            when(productRepository.save(any())).thenReturn(product);
            when(productMapper.toResponse(any())).thenReturn(mock(ProductResponse.class));

            // When
            productService.createProduct(createRequest);

            // Then
            assertThat(product.isDeleted()).isFalse();
            assertThat(product.getActive()).isTrue();
            verify(productRepository).save(product);
        }

        @Test
        void createProduct_AlreadyExists_ThrowsException() {
            // Given
            when(productRepository.findByName("Popcorn")).thenReturn(Optional.of(product));

            // When & Then
            assertThatThrownBy(() -> productService.createProduct(createRequest))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_ALREADY_EXISTS);
        }

        @Test
        void updateProduct_Success() {
            // Given
            UpdateProductRequest updateRequest = new UpdateProductRequest("Caramel Popcorn", BigDecimal.valueOf(60000), null, true);
            when(productRepository.findById("prod-001")).thenReturn(Optional.of(product));
            when(productRepository.findByName("Caramel Popcorn")).thenReturn(Optional.empty());
            when(productMapper.toResponse(any())).thenReturn(mock(ProductResponse.class));

            // When
            productService.updateProduct("prod-001", updateRequest);

            // Then
            verify(productMapper).update(product, updateRequest);
            assertThat(product.getName()).isEqualTo("Caramel Popcorn");
        }

        @Test
        void updateProduct_AlreadyExists_ThrowsException() {
            // Given
            UpdateProductRequest updateRequest = new UpdateProductRequest("Existing", null, null, null);
            Product other = new Product();
            other.setId("other");
            
            when(productRepository.findById("prod-001")).thenReturn(Optional.of(product));
            when(productRepository.findByName("Existing")).thenReturn(Optional.of(other));

            // When & Then
            assertThatThrownBy(() -> productService.updateProduct("prod-001", updateRequest))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_ALREADY_EXISTS);
        }

        @Test
        void updateProduct_NotFound_ThrowsException() {
            when(productRepository.findById("unknown")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> productService.updateProduct("unknown", new UpdateProductRequest("n", BigDecimal.ONE, null, null)))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
        }

        @Test
        void deleteProduct_Success() {
            // Given
            when(productRepository.findById("prod-001")).thenReturn(Optional.of(product));

            // When
            productService.deleteProduct("prod-001");

            // Then
            assertThat(product.isDeleted()).isTrue();
            assertThat(product.getActive()).isFalse();
            verify(productRepository).save(product);
        }

        @Test
        void deleteProduct_NotFound_ThrowsException() {
            when(productRepository.findById("unknown")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> productService.deleteProduct("unknown"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
        }

        @Test
        void toggleActiveProduct_Success() {
            // Given
            when(productRepository.findById("prod-001")).thenReturn(Optional.of(product));

            // When
            productService.toggleActiveProduct("prod-001");

            // Then
            assertThat(product.getActive()).isFalse();
            verify(productRepository).save(product);
        }

        @Test
        void toggleActiveProduct_NotFound_ThrowsException() {
            when(productRepository.findById("unknown")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> productService.toggleActiveProduct("unknown"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
        }
    }

    @Nested
    class ListTests {
        @Test
        void getProductById_Success() {
            // Given
            when(productRepository.findById("prod-001")).thenReturn(Optional.of(product));
            when(productMapper.toResponse(any())).thenReturn(mock(ProductResponse.class));

            // When
            ProductResponse response = productService.getProductById("prod-001");

            // Then
            assertThat(response).isNotNull();
        }

        @Test
        void getProductById_NotFound_ThrowsException() {
            when(productRepository.findById("unknown")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> productService.getProductById("unknown"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
        }

        @Test
        void getAllProducts_Success() {
            Page<Product> page = new PageImpl<>(Collections.singletonList(product));
            when(productRepository.findAllByDeletedFalse(any())).thenReturn(page);
            when(productMapper.toResponse(any())).thenReturn(mock(ProductResponse.class));

            PageResponse<ProductResponse> response = productService.getAllProducts(1, 10, "id", "asc", null);
            assertThat(response.getItems()).hasSize(1);
        }

        @Test
        void getAllProducts_WithKeyword_Success() {
            Page<Product> page = new PageImpl<>(Collections.singletonList(product));
            when(productRepository.findByNameContainingIgnoreCaseAndDeletedFalse(any(), any())).thenReturn(page);
            when(productMapper.toResponse(any())).thenReturn(mock(ProductResponse.class));

            PageResponse<ProductResponse> response = productService.getAllProducts(1, 10, "id", "asc", "key");
            assertThat(response.getItems()).hasSize(1);
        }

        @Test
        void getProductActive_Success() {
            Page<Product> page = new PageImpl<>(Collections.singletonList(product));
            when(productRepository.findByActiveTrueAndDeletedFalse(any())).thenReturn(page);
            when(productMapper.toResponse(any())).thenReturn(mock(ProductResponse.class));

            PageResponse<ProductResponse> response = productService.getProductActive(1, 10, "id", "desc");
            assertThat(response.getItems()).hasSize(1);
        }
    }
}
