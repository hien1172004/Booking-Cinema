package org.example.cinemaBooking.unit.service;

import org.example.cinemaBooking.DTO.Request.Promotion.CreatePromotionRequest;
import org.example.cinemaBooking.DTO.Request.Promotion.UpdatePromotionRequest;
import org.example.cinemaBooking.DTO.Response.Promotion.PromotionResponse;
import org.example.cinemaBooking.DTO.Response.ValidationResultResponse;
import org.example.cinemaBooking.Entity.Promotion;
import org.example.cinemaBooking.Entity.UserEntity;
import org.example.cinemaBooking.Exception.AppException;
import org.example.cinemaBooking.Exception.ErrorCode;
import org.example.cinemaBooking.Mapper.PromotionMapper;
import org.example.cinemaBooking.Repository.PromotionRepository;
import org.example.cinemaBooking.Repository.UsedPromotionRepository;
import org.example.cinemaBooking.Repository.UserRepository;
import org.example.cinemaBooking.Service.Promotion.PromotionService;
import org.example.cinemaBooking.Shared.enums.DiscountType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PromotionServiceTest {

    @Mock
    private PromotionRepository promotionRepository;
    @Mock
    private PromotionMapper promotionMapper;
    @Mock
    private UsedPromotionRepository usedPromotionRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PromotionService promotionService;

    private Promotion promotion;
    private CreatePromotionRequest createRequest;

    @BeforeEach
    void setUp() {
        promotion = Promotion.builder()
                .code("SAVE10")
                .name("Save 10%")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(10))
                .maxDiscount(BigDecimal.valueOf(50000))
                .minOrderValue(BigDecimal.valueOf(100000))
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(30))
                .quantity(100)
                .usedQuantity(0)
                .maxUsagePerUser(1)
                .build();
        promotion.setId("promo-001");

        createRequest = new CreatePromotionRequest(
                "SAVE10", "Save 10%", "Desc", DiscountType.PERCENTAGE,
                BigDecimal.valueOf(10), BigDecimal.valueOf(100000), BigDecimal.valueOf(50000),
                100, LocalDate.now().plusDays(1), LocalDate.now().plusDays(10), 1);
    }

    @Nested
    class CreateUpdateTests {
        @Test
        void createPromotion_Success() {
            // Given
            when(promotionRepository.findByCode("SAVE10")).thenReturn(Optional.empty());
            when(promotionMapper.toEntity(any())).thenReturn(promotion);
            when(promotionRepository.save(any())).thenReturn(promotion);
            when(promotionMapper.toResponse(any())).thenReturn(mock(PromotionResponse.class));

            // When
            promotionService.createPromotion(createRequest);

            // Then
            verify(promotionRepository).save(any());
        }

        @Test
        void createPromotion_RestoreDeleted_Success() {
            // Given
            promotion.setDeleted(true);
            when(promotionRepository.findByCode("SAVE10")).thenReturn(Optional.of(promotion));
            when(promotionRepository.save(any())).thenReturn(promotion);
            when(promotionMapper.toResponse(any())).thenReturn(mock(PromotionResponse.class));

            // When
            promotionService.createPromotion(createRequest);

            // Then
            assertThat(promotion.isDeleted()).isFalse();
            verify(promotionRepository).save(promotion);
        }

        @Test
        void createPromotion_AlreadyExists_ThrowsException() {
            // Given
            when(promotionRepository.findByCode("SAVE10")).thenReturn(Optional.of(promotion));

            // When & Then
            assertThatThrownBy(() -> promotionService.createPromotion(createRequest))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PROMOTION_ALREADY_EXISTS);
        }

        @Test
        void createPromotion_InvalidDate_ThrowsException() {
            // Given
            CreatePromotionRequest invalidRequest = new CreatePromotionRequest(
                    "SAVE10", "Save 10%", "Desc", DiscountType.PERCENTAGE,
                    BigDecimal.valueOf(10), BigDecimal.valueOf(100000), BigDecimal.valueOf(50000),
                    100, LocalDate.now().plusDays(5), LocalDate.now().plusDays(1), 1);

            // When & Then
            assertThatThrownBy(() -> promotionService.createPromotion(invalidRequest))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_DATE_RANGE);
        }

        @Test
        void validateDate_StartDateInPast_ThrowsException() {
            // Given
            CreatePromotionRequest pastRequest = new CreatePromotionRequest(
                    "CODE", "Name", "Desc", DiscountType.FIXED, BigDecimal.valueOf(100),
                    null, null, 10, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1), 1);

            // When & Then
            assertThatThrownBy(() -> promotionService.createPromotion(pastRequest))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.START_DATE_INVALID);
        }

        @Test
        void validatePercentage_InvalidValue_ThrowsException() {
            // Given
            CreatePromotionRequest req = new CreatePromotionRequest(
                    "CODE", "Name", "Desc", DiscountType.PERCENTAGE, BigDecimal.valueOf(150), // > 100
                    null, null, 10, LocalDate.now(), LocalDate.now().plusDays(1), 1);

            // When & Then
            assertThatThrownBy(() -> promotionService.createPromotion(req))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DISCOUNT_VALUE_INVALID);
        }

        @Test
        void validateFixed_WithMaxDiscount_ThrowsException() {
            // Given
            CreatePromotionRequest req = new CreatePromotionRequest(
                    "CODE", "Name", "Desc", DiscountType.FIXED, BigDecimal.valueOf(100000),
                    null, BigDecimal.valueOf(10000), // fixed but with maxDiscount
                    10, LocalDate.now(), LocalDate.now().plusDays(1), 1);

            // When & Then
            assertThatThrownBy(() -> promotionService.createPromotion(req))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MAX_DISCOUNT_NOT_ALLOWED);
        }

        @Test
        void updatePromotion_Success() {
            // Given
            UpdatePromotionRequest req = new UpdatePromotionRequest(
                    "Name", "Desc", DiscountType.PERCENTAGE, BigDecimal.valueOf(10),
                    BigDecimal.valueOf(100000), BigDecimal.valueOf(50000),
                    100, LocalDate.now().plusDays(5), LocalDate.now().plusDays(20), 1);

            when(promotionRepository.findById("promo-001")).thenReturn(Optional.of(promotion));
            when(promotionRepository.save(any())).thenReturn(promotion);
            when(promotionMapper.toResponse(any())).thenReturn(mock(PromotionResponse.class));

            // When
            promotionService.updatePromotion("promo-001", req);

            // Then
            verify(promotionRepository).save(promotion);
        }
    }

    @Nested
    class ActionTests {
        @Test
        void deletePromotion_Success() {
            // Given
            when(promotionRepository.findById("promo-001")).thenReturn(Optional.of(promotion));
            when(promotionRepository.save(any())).thenReturn(promotion);

            // When
            promotionService.deletePromotion("promo-001");

            // Then
            assertThat(promotion.isDeleted()).isTrue();
            verify(promotionRepository).save(promotion);
        }

        @Test
        void deletePromotion_NotFound_ThrowsException() {
            when(promotionRepository.findById("unknown")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> promotionService.deletePromotion("unknown"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PROMOTION_NOT_FOUND);
        }

        @Test
        void deletePromotion_AlreadyDeleted_ThrowsException() {
            // Given
            promotion.setDeleted(true);
            when(promotionRepository.findById("promo-001")).thenReturn(Optional.of(promotion));

            // When & Then
            assertThatThrownBy(() -> promotionService.deletePromotion("promo-001"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PROMOTION_ALREADY_DELETED);
        }

        @Test
        void getPromotionById_Success() {
            when(promotionRepository.findById("promo-001")).thenReturn(Optional.of(promotion));
            when(promotionMapper.toResponse(promotion)).thenReturn(mock(PromotionResponse.class));
            assertThat(promotionService.getPromotionById("promo-001")).isNotNull();
        }

        @Test
        void getPromotionByCode_NotFound_ThrowsException() {
            when(promotionRepository.findByCode("GHOST")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> promotionService.getPromotionByCode("GHOST"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PROMOTION_NOT_FOUND);
        }
    }

    @Nested
    class PreviewPromotionTests {
        @Test
        void previewPromotion_FixedValue_Success() {
            // Given
            promotion.setDiscountType(DiscountType.FIXED);
            promotion.setDiscountValue(BigDecimal.valueOf(20000));
            promotion.setMaxDiscount(null);
            when(promotionRepository.findByCode("FIXED")).thenReturn(Optional.of(promotion));
            when(usedPromotionRepository.getTotalUsageCountByUserAndPromotion(any(), any())).thenReturn(0);

            // When
            ValidationResultResponse result = promotionService.previewPromotion("FIXED", "u", BigDecimal.valueOf(100000));

            // Then
            assertThat(result.discountAmount().compareTo(BigDecimal.valueOf(20000))).isEqualTo(0);
        }

        @Test
        void previewPromotion_AlreadyDeleted_ThrowsException() {
            promotion.setDeleted(true);
            when(promotionRepository.findByCode("SAVE10")).thenReturn(Optional.of(promotion));

            assertThatThrownBy(() -> promotionService.previewPromotion("SAVE10", "u", BigDecimal.valueOf(200000)))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PROMOTION_ALREADY_DELETED);
        }

        @Test
        void previewPromotion_OutOfStock_ThrowsException() {
            promotion.setQuantity(100);
            promotion.setUsedQuantity(100);
            when(promotionRepository.findByCode("SAVE10")).thenReturn(Optional.of(promotion));

            assertThatThrownBy(() -> promotionService.previewPromotion("SAVE10", "u", BigDecimal.valueOf(200000)))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PROMOTION_OUT_OF_STOCK);
        }

        @Test
        void previewPromotion_NotStarted_ThrowsException() {
            // Given
            promotion.setStartDate(LocalDate.now().plusDays(1));
            when(promotionRepository.findByCode("SAVE10")).thenReturn(Optional.of(promotion));

            // When & Then
            assertThatThrownBy(() -> promotionService.previewPromotion("SAVE10", "u", BigDecimal.valueOf(200000)))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PROMOTION_NOT_STARTED);
        }

        @Test
        void previewPromotion_Expired_ThrowsException() {
            // Given
            promotion.setEndDate(LocalDate.now().minusDays(1));
            when(promotionRepository.findByCode("SAVE10")).thenReturn(Optional.of(promotion));

            // When & Then
            assertThatThrownBy(() -> promotionService.previewPromotion("SAVE10", "u", BigDecimal.valueOf(200000)))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PROMOTION_EXPIRED);
        }

        @Test
        void previewPromotion_MinOrderNotMet_ThrowsException() {
            // Given
            when(promotionRepository.findByCode("SAVE10")).thenReturn(Optional.of(promotion));

            // When & Then
            assertThatThrownBy(() -> promotionService.previewPromotion("SAVE10", "user-001", BigDecimal.valueOf(50000)))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MIN_ORDER_VALUE_INVALID);
        }

        @Test
        void previewPromotion_UsageLimitExceeded_ThrowsException() {
            // Given
            when(promotionRepository.findByCode("SAVE10")).thenReturn(Optional.of(promotion));
            when(usedPromotionRepository.getTotalUsageCountByUserAndPromotion(any(), any())).thenReturn(1);

            // When & Then
            assertThatThrownBy(
                    () -> promotionService.previewPromotion("SAVE10", "user-001", BigDecimal.valueOf(200000)))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PROMOTION_USAGE_LIMIT_EXCEEDED);
        }
    }

    @Nested
    class ApplyPromotionTests {
        @Test
        void applyPromotion_UserNotFound_ThrowsException() {
            when(userRepository.findById("ghost")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> promotionService.applyPromotion("p", "ghost"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
        }

        @Test
        void applyPromotion_PromotionNotFound_ThrowsException() {
            when(userRepository.findById(any())).thenReturn(Optional.of(new UserEntity()));
            when(promotionRepository.findById("ghost")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> promotionService.applyPromotion("ghost", "u"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PROMOTION_NOT_FOUND);
        }

        @Test
        void applyPromotion_Success() {
            // Given
            UserEntity user = new UserEntity();
            user.setId("user-001");
            when(userRepository.findById("user-001")).thenReturn(Optional.of(user));
            when(promotionRepository.findById("promo-001")).thenReturn(Optional.of(promotion));
            when(promotionRepository.increaseUsedQuantityIfAvailable("promo-001")).thenReturn(1);
            when(usedPromotionRepository.findByUserIdAndPromotionId(any(), any())).thenReturn(Optional.empty());

            // When
            promotionService.applyPromotion("promo-001", "user-001");

            // Then
            verify(promotionRepository).increaseUsedQuantityIfAvailable("promo-001");
            verify(usedPromotionRepository).save(any());
        }

        @Test
        void applyPromotion_ExistingUsage_Increments() {
            // Given
            UserEntity user = new UserEntity();
            user.setId("user-001");
            org.example.cinemaBooking.Entity.UsedPromotion used = new org.example.cinemaBooking.Entity.UsedPromotion();
            used.setUsageCount(1);
            when(userRepository.findById("user-001")).thenReturn(Optional.of(user));
            when(promotionRepository.findById("promo-001")).thenReturn(Optional.of(promotion));
            when(promotionRepository.increaseUsedQuantityIfAvailable("promo-001")).thenReturn(1);
            when(usedPromotionRepository.findByUserIdAndPromotionId(any(), any())).thenReturn(Optional.of(used));

            // When
            promotionService.applyPromotion("promo-001", "user-001");

            // Then
            assertThat(used.getUsageCount()).isEqualTo(2);
            verify(usedPromotionRepository).save(used);
        }

        @Test
        void applyPromotion_ConcurrencyFallback_Success() {
            // Scenario: updateUsedPromotion save throws exception (duplicate), then fallback finds it and increments
            UserEntity user = new UserEntity(); user.setId("user-001");
            org.example.cinemaBooking.Entity.UsedPromotion used = new org.example.cinemaBooking.Entity.UsedPromotion();
            used.setUsageCount(1);
            
            when(userRepository.findById(any())).thenReturn(Optional.of(user));
            when(promotionRepository.findById(any())).thenReturn(Optional.of(promotion));
            when(promotionRepository.increaseUsedQuantityIfAvailable(any())).thenReturn(1);
            
            // Interaction: find returns empty first, then finding in catch block returns it
            when(usedPromotionRepository.findByUserIdAndPromotionId(any(), any()))
                .thenReturn(Optional.empty(), Optional.of(used));

            // Save throws error once
            doThrow(new RuntimeException("Conflict")).doAnswer(i -> i.getArgument(0)).when(usedPromotionRepository).save(any());

            // When
            promotionService.applyPromotion("p", "u");

            // Then
            assertThat(used.getUsageCount()).isEqualTo(2);
            verify(usedPromotionRepository, times(2)).save(any());
        }

        @Test
        void applyPromotion_OutOfStock_ThrowsException() {
            // Given
            UserEntity user = new UserEntity();
            user.setId("user-001");
            when(userRepository.findById("user-001")).thenReturn(Optional.of(user));
            when(promotionRepository.findById("promo-001")).thenReturn(Optional.of(promotion));
            when(promotionRepository.increaseUsedQuantityIfAvailable("promo-001")).thenReturn(0);

            // When & Then
            assertThatThrownBy(() -> promotionService.applyPromotion("promo-001", "user-001"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PROMOTION_OUT_OF_STOCK);
        }
    }

    @Nested
    class ListTests {
        @Test
        void getPromotions_Success() {
            // Given
            org.springframework.data.domain.Page<Promotion> page = new org.springframework.data.domain.PageImpl<>(java.util.Collections.singletonList(promotion));
            when(promotionRepository.findWithFilters(any(), any(), any(), any(), any(), any(), any())).thenReturn(page);
            when(promotionMapper.toResponse(any())).thenReturn(mock(PromotionResponse.class));
            org.example.cinemaBooking.DTO.Request.Promotion.PromotionFilterRequest request = new org.example.cinemaBooking.DTO.Request.Promotion.PromotionFilterRequest(null, null, null, null, null, null);

            // When
            org.example.cinemaBooking.Shared.response.PageResponse<PromotionResponse> response = promotionService.getPromotions(1, 10, "id", "desc", request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getItems()).hasSize(1);
        }

        @Test
        void getActivePromotions_Success() {
            // Given
            org.springframework.data.domain.Page<Promotion> page = new org.springframework.data.domain.PageImpl<>(java.util.Collections.singletonList(promotion));
            when(promotionRepository.findActivePromotions(any(), any())).thenReturn(page);
            when(promotionMapper.toResponse(any())).thenReturn(mock(PromotionResponse.class));

            // When
            org.example.cinemaBooking.Shared.response.PageResponse<PromotionResponse> response = promotionService.getActivePromotions(1, 10, "id", "asc");

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getItems()).hasSize(1);
        }
    }
}
