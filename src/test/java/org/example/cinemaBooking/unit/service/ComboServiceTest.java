package org.example.cinemaBooking.unit.service;

import org.example.cinemaBooking.DTO.Request.Combo.ComboItemRequest;
import org.example.cinemaBooking.DTO.Request.Combo.CreateComboRequest;
import org.example.cinemaBooking.DTO.Request.Combo.UpdateComboRequest;
import org.example.cinemaBooking.DTO.Response.Combo.ComboResponse;
import org.example.cinemaBooking.Entity.Combo;
import org.example.cinemaBooking.Entity.Product;
import org.example.cinemaBooking.Exception.AppException;
import org.example.cinemaBooking.Exception.ErrorCode;
import org.example.cinemaBooking.Mapper.ComboMapper;
import org.example.cinemaBooking.Repository.ComboRepository;
import org.example.cinemaBooking.Repository.ProductRepository;
import org.example.cinemaBooking.Service.Product.ComboService;
import org.example.cinemaBooking.Shared.response.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ComboServiceTest {

    @Mock
    private ComboRepository comboRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private ComboMapper comboMapper;

    @InjectMocks
    private ComboService comboService;

    private Combo combo;
    private Product product;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId("prod-001");
        product.setName("Coke");
        product.setDeleted(false);

        combo = new Combo();
        combo.setId("combo-001");
        combo.setName("Couple Combo");
        combo.setActive(true);
    }

    @Nested
    class CreateUpdateTests {
        @Test
        void createCombo_Success() {
            // Given
            ComboItemRequest itemReq = new ComboItemRequest("prod-001", 2);
            CreateComboRequest request = new CreateComboRequest("Couple Combo", BigDecimal.valueOf(100000), "image", "Desc", Collections.singletonList(itemReq));

            when(comboRepository.findByName("Couple Combo")).thenReturn(Optional.empty());
            when(comboMapper.toEntity(any())).thenReturn(combo);
            when(productRepository.findAllById(any())).thenReturn(Collections.singletonList(product));
            when(comboRepository.save(any())).thenReturn(combo);
            when(comboMapper.toResponse(any())).thenReturn(mock(ComboResponse.class));

            // When
            comboService.createCombo(request);

            // Then
            verify(comboRepository).save(any());
        }

        @Test
        void createCombo_RestoreDeleted_Success() {
            // Given
            combo.setDeleted(true);
            combo.setActive(false);
            ComboItemRequest itemReq = new ComboItemRequest("prod-001", 2);
            CreateComboRequest request = new CreateComboRequest("Couple Combo", BigDecimal.valueOf(100000), "image", "Desc", Collections.singletonList(itemReq));

            when(comboRepository.findByName("Couple Combo")).thenReturn(Optional.of(combo));
            when(productRepository.findAllById(any())).thenReturn(Collections.singletonList(product));
            when(comboRepository.save(any())).thenReturn(combo);
            when(comboMapper.toResponse(any())).thenReturn(mock(ComboResponse.class));

            // When
            comboService.createCombo(request);

            // Then
            assertThat(combo.isDeleted()).isFalse();
            assertThat(combo.getActive()).isTrue();
            verify(comboRepository).save(combo);
        }

        @Test
        void createCombo_ProductNotFound_ThrowsException() {
            // Given
            ComboItemRequest itemReq = new ComboItemRequest("invalid", 1);
            CreateComboRequest request = new CreateComboRequest("Combo", BigDecimal.valueOf(50), "img", "Desc", Collections.singletonList(itemReq));

            when(comboRepository.findByName(any())).thenReturn(Optional.empty());
            when(comboMapper.toEntity(any())).thenReturn(combo);
            when(productRepository.findAllById(any())).thenReturn(Collections.emptyList());

            // When & Then
            assertThatThrownBy(() -> comboService.createCombo(request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
        }

        @Test
        void createCombo_DuplicateProducts_ThrowsException() {
            // Given
            ComboItemRequest item1 = new ComboItemRequest("prod-001", 1);
            ComboItemRequest item2 = new ComboItemRequest("prod-001", 1);
            CreateComboRequest request = new CreateComboRequest("Combo", BigDecimal.valueOf(50), "img", "Desc", Arrays.asList(item1, item2));

            // When & Then
            assertThatThrownBy(() -> comboService.createCombo(request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_PRODUCT_IN_COMBO);
        }

        @Test
        void updateCombo_Success() {
            // Given
            UpdateComboRequest request = new UpdateComboRequest("New Name", BigDecimal.valueOf(120), "new desc", "new img", null, true);
            when(comboRepository.findByIdWithDetail("combo-001")).thenReturn(Optional.of(combo));
            when(comboRepository.findByName("New Name")).thenReturn(Optional.empty());
            when(comboRepository.save(any())).thenReturn(combo);
            when(comboMapper.toResponse(any())).thenReturn(mock(ComboResponse.class));

            // When
            comboService.updateCombo("combo-001", request);

            // Then
            verify(comboMapper).updateCombo(any(), eq(combo));
            verify(comboRepository).save(combo);
        }

        @Test
        void updateCombo_AlreadyExists_ThrowsException() {
            // Given
            UpdateComboRequest request = new UpdateComboRequest("Existing", null, null, null, null, null);
            Combo other = new Combo();
            other.setId("other");
            other.setDeleted(false);

            when(comboRepository.findByIdWithDetail("combo-001")).thenReturn(Optional.of(combo));
            when(comboRepository.findByName("Existing")).thenReturn(Optional.of(other));

            // When & Then
            assertThatThrownBy(() -> comboService.updateCombo("combo-001", request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMBO_ALREADY_EXISTS);
        }

        @Test
        void createCombo_AlreadyExists_ThrowsException() {
            when(comboRepository.findByName(any())).thenReturn(Optional.of(combo));
            ComboItemRequest itemReq = new ComboItemRequest("prod-001", 2);
            CreateComboRequest request = new CreateComboRequest("Couple Combo", BigDecimal.valueOf(100), "img", "Desc", Collections.singletonList(itemReq));

            assertThatThrownBy(() -> comboService.createCombo(request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMBO_ALREADY_EXISTS);
        }

        @Test
        void updateCombo_NotFound_ThrowsException() {
            when(comboRepository.findByIdWithDetail(any())).thenReturn(Optional.empty());
            assertThatThrownBy(() -> comboService.updateCombo("unknown", new UpdateComboRequest("n", null, null, null, null, null)))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMBO_NOT_FOUND);
        }

        @Test
        void deleteCombo_Success() {
            when(comboRepository.findById("combo-001")).thenReturn(Optional.of(combo));
            comboService.deleteCombo("combo-001");
            assertThat(combo.isDeleted()).isTrue();
            assertThat(combo.getActive()).isFalse();
            verify(comboRepository).save(combo);
        }

        @Test
        void deleteCombo_NotFound_ThrowsException() {
            when(comboRepository.findById(any())).thenReturn(Optional.empty());
            assertThatThrownBy(() -> comboService.deleteCombo("unknown"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMBO_NOT_FOUND);
        }
    }

    @Nested
    class ListTests {
        @Test
        void getCombos_PageOne_Success() {
            when(comboRepository.findByDeletedFalse(any())).thenReturn(new PageImpl<>(Collections.singletonList(combo)));
            when(comboMapper.toResponse(any())).thenReturn(mock(ComboResponse.class));
            comboService.getCombos(1, 10, null, "desc", "id");
            verify(comboRepository).findByDeletedFalse(argThat(p -> p.getPageNumber() == 0));
        }

        @Test
        void getCombos_WithKeyword() {
            // Given
            when(comboRepository.findByNameContainingIgnoreCaseAndDeletedFalse(any(), any())).thenReturn(new PageImpl<>(Collections.singletonList(combo)));
            when(comboMapper.toResponse(any())).thenReturn(mock(ComboResponse.class));

            // When
            comboService.getCombos(1, 10, "keyword", "asc", "id");

            // Then
            verify(comboRepository).findByNameContainingIgnoreCaseAndDeletedFalse(eq("keyword"), any());
        }

        @Test
        void getAllCombosActive_Success() {
            // Given
            when(comboRepository.findByActiveTrueAndDeletedFalse(any())).thenReturn(new PageImpl<>(Collections.singletonList(combo)));
            when(comboMapper.toResponse(any())).thenReturn(mock(ComboResponse.class));

            // When
            comboService.getAllCombosActive(1, 10, "id", "asc");

            // Then
            verify(comboRepository).findByActiveTrueAndDeletedFalse(any());
        }
    }

    @Nested
    class DetailTests {
        @Test
        void getComboById_Success() {
            // Given
            when(comboRepository.findByIdWithDetail("combo-001")).thenReturn(Optional.of(combo));
            when(comboMapper.toResponse(any())).thenReturn(mock(ComboResponse.class));

            // When
            ComboResponse response = comboService.getComboById("combo-001");

            // Then
            assertThat(response).isNotNull();
            verify(comboRepository).findByIdWithDetail("combo-001");
        }

        @Test
        void getComboById_NotFound_ThrowsException() {
            when(comboRepository.findByIdWithDetail(any())).thenReturn(Optional.empty());
            assertThatThrownBy(() -> comboService.getComboById("unknown"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMBO_NOT_FOUND);
        }

        @Test
        void toggleActiveCombo_Success() {
            // Given
            when(comboRepository.findById("combo-001")).thenReturn(Optional.of(combo));

            // When
            comboService.toggleActiveCombo("combo-001");

            // Then
            assertThat(combo.getActive()).isFalse();
            verify(comboRepository).save(combo);
        }

        @Test
        void toggleActiveCombo_NotFound_ThrowsException() {
            when(comboRepository.findById(any())).thenReturn(Optional.empty());
            assertThatThrownBy(() -> comboService.toggleActiveCombo("unknown"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMBO_NOT_FOUND);
        }
    }
}
