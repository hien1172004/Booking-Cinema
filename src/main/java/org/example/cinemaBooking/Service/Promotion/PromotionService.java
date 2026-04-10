package org.example.cinemaBooking.Service.Promotion;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.cinemaBooking.DTO.Request.Promotion.CreatePromotionRequest;
import org.example.cinemaBooking.DTO.Request.Promotion.PromotionFilterRequest;
import org.example.cinemaBooking.DTO.Request.Promotion.UpdatePromotionRequest;
import org.example.cinemaBooking.DTO.Response.Promotion.PromotionResponse;
import org.example.cinemaBooking.DTO.Response.ValidationResultResponse;
import org.example.cinemaBooking.Entity.Promotion;
import org.example.cinemaBooking.Entity.UsedPromotion;
import org.example.cinemaBooking.Entity.UserEntity;
import org.example.cinemaBooking.Exception.AppException;
import org.example.cinemaBooking.Exception.ErrorCode;
import org.example.cinemaBooking.Mapper.PromotionMapper;
import org.example.cinemaBooking.Repository.PromotionRepository;
import org.example.cinemaBooking.Repository.UsedPromotionRepository;
import org.example.cinemaBooking.Repository.UserRepository;
import org.example.cinemaBooking.Shared.response.PageResponse;
import org.example.cinemaBooking.Shared.enums.DiscountType;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class PromotionService {
    PromotionRepository promotionRepository;
    PromotionMapper promotionMapper;
    UsedPromotionRepository usedPromotionRepository;
    UserRepository userRepository;
    /**
     * Validate promotion data before create/update
     */
    private void validatePromotionData(LocalDate startDate, LocalDate endDate,
                                       DiscountType discountType, BigDecimal discountValue,
                                       BigDecimal maxDiscount, Integer quantity, Integer usedQuantity) {

        validateDate(startDate, endDate);
        validateDiscount(discountType, discountValue, maxDiscount);
        validateQuantity(quantity, usedQuantity);
    }
    private void validateDate(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE);
        }

        if (startDate.isBefore(LocalDate.now())) {
            throw new AppException(ErrorCode.START_DATE_INVALID);
        }
    }
    private void validateDiscount(DiscountType type,
                                  BigDecimal value,
                                  BigDecimal maxDiscount) {

        if (type == DiscountType.PERCENTAGE) {
            validatePercentage(value, maxDiscount);
        } else {
            validateFixed(value, maxDiscount);
        }
    }
    private void validatePercentage(BigDecimal value, BigDecimal maxDiscount) {
        if (value.compareTo(BigDecimal.ZERO) <= 0 ||
                value.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new AppException(ErrorCode.DISCOUNT_VALUE_INVALID);
        }

        if (maxDiscount != null && maxDiscount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(ErrorCode.MAX_DISCOUNT_INVALID);
        }
    }
    private void validateFixed(BigDecimal value, BigDecimal maxDiscount) {
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(ErrorCode.DISCOUNT_VALUE_INVALID);
        }

        if (maxDiscount != null) {
            throw new AppException(ErrorCode.MAX_DISCOUNT_NOT_ALLOWED);
        }
    }
    private void validateQuantity(Integer quantity, Integer usedQuantity) {
        if (quantity != null && usedQuantity != null && quantity < usedQuantity) {
            throw new AppException(ErrorCode.QUANTITY_LESS_THAN_USED);
        }
    }


    /**
     * Tạo mới một chương trình khuyến mãi.
     * <p>Xoá bộ đệm các danh sách khuyến mãi để cập nhật danh sách mới.</p>
     *
     * @param request Dữ liệu khuyến mãi
     * @return PromotionResponse Thông tin khuyến mãi sau khi tạo
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "promotions", allEntries = true),
            @CacheEvict(value = "promotions-active", allEntries = true)
    })
    public PromotionResponse createPromotion(CreatePromotionRequest request) {

        validatePromotionData(request.startDate(), request.endDate(), request.discountType(), request.discountValue(),
                request.maxDiscount(), request.quantity(), 0);

        Optional<Promotion> existingPromotion = promotionRepository.findByCode(request.code());

        Promotion promotion;
        if(existingPromotion.isPresent()) {
            promotion = existingPromotion.get();

            if(!promotion.isDeleted()){
                throw new AppException(ErrorCode.PROMOTION_ALREADY_EXISTS);
            }
            promotion.restore();
            promotionMapper.update(promotion,
                    new UpdatePromotionRequest(
                            request.name(),
                            request.description(),
                            request.discountType(),
                            request.discountValue(),
                            request.minOrderValue(),
                            request.maxDiscount(),
                            request.quantity(),
                            request.startDate(),
                            request.endDate(),
                            request.maxUsagePerUser()
                    )
            );
        }
        else {
            promotion = promotionMapper.toEntity(request);
        }
        Promotion savedPromotion = promotionRepository.save(promotion);
        return promotionMapper.toResponse(savedPromotion);
    }


    /**
     * Cập nhật thông tin chương trình khuyến mãi.
     * <p>Xoá bộ đệm danh sách và chi tiết khuyến mãi tương ứng.</p>
     *
     * @param id ID của khuyến mãi cần cập nhật
     * @param request Dữ liệu khuyến mãi mới
     * @return PromotionResponse Thông tin sau khi cập nhật
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "promotions", allEntries = true),
            @CacheEvict(value = "promotions-active", allEntries = true),
            @CacheEvict(value = "promotion", key = "#id"),
            @CacheEvict(value = "promotion-code", allEntries = true)
    })
    public PromotionResponse updatePromotion(String id, UpdatePromotionRequest  request) {

        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROMOTION_NOT_FOUND));

        promotionMapper.update(promotion, request);
        validatePromotionData(promotion.getStartDate(), promotion.getEndDate(), promotion.getDiscountType(),
                promotion.getDiscountValue(), promotion.getMaxDiscount(), promotion.getQuantity(), promotion.getUsedQuantity());

        Promotion updatedPromotion = promotionRepository.save(promotion);
        return promotionMapper.toResponse(updatedPromotion);
    }

    /**
     * Xoá mềm chương trình khuyến mãi.
     * <p>Xóa bộ đệm các danh sách và chi tiết khuyến mãi liên quan.</p>
     *
     * @param id ID khuyến mãi
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "promotions", allEntries = true),
            @CacheEvict(value = "promotions-active", allEntries = true),
            @CacheEvict(value = "promotion", key = "#id"),
            @CacheEvict(value = "promotion-code", allEntries = true)
    })
    public void deletePromotion(String id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROMOTION_NOT_FOUND));

        if (promotion.isDeleted()) {
            throw new AppException(ErrorCode.PROMOTION_ALREADY_DELETED);
        }

        promotion.softDelete();
        promotionRepository.save(promotion);
    }

    /**
     * Lấy thông tin chi tiết khuyến mãi theo ID.
     * <p>Kết quả được lưu vào cache "promotion".</p>
     *
     * @param id ID khuyến mãi
     * @return PromotionResponse Thông tin chi tiết
     */
    @Cacheable(value = "promotion", key = "#id")
    public PromotionResponse getPromotionById(String id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROMOTION_NOT_FOUND));

        return promotionMapper.toResponse(promotion);
    }

    /**
     * Lấy thông tin chi tiết khuyến mãi theo Code.
     * <p>Kết quả được lưu vào cache "promotion-code".</p>
     *
     * @param code Mã khuyến mãi
     * @return PromotionResponse Thông tin chi tiết
     */
    @Cacheable(value = "promotion-code", key = "#code")
    public PromotionResponse getPromotionByCode(String code) {
        Promotion promotion = promotionRepository.findByCode(code)
                .orElseThrow(() -> new AppException(ErrorCode.PROMOTION_NOT_FOUND));

        return promotionMapper.toResponse(promotion);
    }

    /**
     * Lấy danh sách khuyến mãi dành cho Admin (hỗ trợ phân trang, lọc).
     * <p>Kết quả theo bộ lọc được lưu cache "promotions".</p>
     *
     * @param page Số trang
     * @param size Kích thước trang
     * @param sortBy Cột sắp xếp
     * @param direction Hướng sắp xếp
     * @param request Bộ lọc tìm kiếm
     * @return PageResponse Danh sách khuyến mãi
     */
    @Cacheable(value = "promotions", key = "#page + '-' + #size + '-' + #sortBy + '-' + #direction + '-' + (#request.code() ?: '') + '-' + (#request.name() ?: '')")
    public PageResponse<PromotionResponse> getPromotions(
            int page,
            int size,
            String sortBy,
            String direction,
            PromotionFilterRequest request
            ) {
        int pageNumber = Math.max(0, page - 1);
        Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(pageNumber, size, Sort.by(sortDirection, sortBy));

        Page<Promotion> promotionPage = promotionRepository.findWithFilters(
                request.code(),
                request.name(),
                request.startDate(),
                request.endDate(),
                request.minOrderValue(),
                request.maxOrderValue(),
                pageable
        );
        List<PromotionResponse> promotionResponses = promotionPage.getContent().stream()
                .map(promotionMapper::toResponse)
                .toList();

        return PageResponse.<PromotionResponse>builder()
                .page(page)
                .size(size)
                .totalElements(promotionPage.getTotalElements())
                .totalPages(promotionPage.getTotalPages())
                .items(promotionResponses)
                .build();
    }


    /**
     * Lấy danh sách khuyến mãi đang khả dụng (Active) để hiển thị cho người dùng.
     * <p>Được cache lại "promotions-active".</p>
     *
     * @param page Số trang
     * @param size Kích thước trang
     * @param sortBy Cột sắp xếp
     * @param direction Hướng sắp xếp
     * @return PageResponse Danh sách khuyến mãi hiển thị
     */
    @Cacheable(value = "promotions-active", key = "#page + '-' + #size + '-' + #sortBy + '-' + #direction")
    public PageResponse<PromotionResponse> getActivePromotions(int page, int size, String sortBy, String direction) {
        int pageNumber = Math.max(0, page - 1);
        Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(pageNumber, size, Sort.by(sortDirection, sortBy));
        LocalDate now = LocalDate.now();
        Page<Promotion> promotionPage = promotionRepository.findActivePromotions(now, pageable);

        List<PromotionResponse> promotionResponses = promotionPage.getContent().stream()
                .map(promotionMapper::toResponse)
                .toList();

        return PageResponse.<PromotionResponse>builder()
                .page(page)
                .size(size)
                .totalElements(promotionPage.getTotalElements())
                .totalPages(promotionPage.getTotalPages())
                .items(promotionResponses)
                .build();
    }

    /**
     * Xem trước (Preview) thông tin khuyến mãi áp dụng cho đơn hàng để lấy số tiền giảm.
     */
    @Transactional
    public ValidationResultResponse previewPromotion(String code, String userId, BigDecimal orderValue) {
        log.info("User {} using promotion: {}", userId, code);

        // 1. Validate promotion code
        Promotion promotion = promotionRepository.findByCode(code)
                .orElseThrow(() -> new AppException(ErrorCode.PROMOTION_NOT_FOUND));
        // Kiểm tra promotion có hợp lệ không
        validatePromotionForUse(promotion, orderValue);

        // 2. Kiểm tra giới hạn sử dụng của user
        int userUsageCount = getUserUsageCount(userId, promotion.getId());
        if ( promotion.getMaxUsagePerUser() != null && userUsageCount >= promotion.getMaxUsagePerUser()) {
            throw new AppException(ErrorCode.PROMOTION_USAGE_LIMIT_EXCEEDED);
        }

        // 3. Tính số tiền giảm
        BigDecimal discountAmount = calculateDiscount(promotion, orderValue);
        BigDecimal finalAmount = orderValue.subtract(discountAmount);

        log.info("User {} successfully used promotion: {}", userId, code);

        return ValidationResultResponse.builder()
                .valid(true)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .promotionCode(code)
                .promotionName(promotion.getName())
                .build();
    }

    /**
     * Áp dụng khuyến mãi và tăng số lượng đã sử dụng (Dùng trong lúc tạo Đơn hàng/Thanh toán).
     * <p>Do số lượng thay đổi nên phải xóa bộ đệm thông tin khuyến mãi tránh hiển thị sai số lượng tồn.</p>
     *
     * @param promotionId ID của khuyến mãi
     * @param userId      ID người dùng dùng mã
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "promotions", allEntries = true),
            @CacheEvict(value = "promotions-active", allEntries = true),
            @CacheEvict(value = "promotion", key = "#promotionId"),
            @CacheEvict(value = "promotion-code", allEntries = true)
    })
    public void applyPromotion(String promotionId, String userId) {

        // 1. Validate tồn tại trước
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new AppException(ErrorCode.PROMOTION_NOT_FOUND));

        // 2. Atomic update quantity
        int updatedRows = promotionRepository.increaseUsedQuantityIfAvailable(promotionId);

        if (updatedRows == 0) {
            throw new AppException(ErrorCode.PROMOTION_OUT_OF_STOCK);
        }

        // 3. Update usage per user
        updateUsedPromotion(user, promotion);
    }


    private void updateUsedPromotion(UserEntity user, Promotion promotion) {
        try {
            Optional<UsedPromotion> existing =
                    usedPromotionRepository.findByUserIdAndPromotionId(user.getId(), promotion.getId());

            if (existing.isPresent()) {
                UsedPromotion used = existing.get();
                used.setUsageCount(used.getUsageCount() + 1);
                usedPromotionRepository.save(used);
            } else {
                UsedPromotion newUsed = UsedPromotion.builder()
                        .user(user)
                        .promotion(promotion)
                        .usageCount(1)
                        .build();

                usedPromotionRepository.save(newUsed);
            }
        } catch (Exception e) {
            // fallback nếu bị duplicate
            UsedPromotion existing = usedPromotionRepository
                    .findByUserIdAndPromotionId(user.getId(), promotion.getId())
                    .orElseThrow();

            existing.setUsageCount(existing.getUsageCount() + 1);
            usedPromotionRepository.save(existing);
        }
    }

    // Helper method: Validate promotion trước khi sử dụng
    private void validatePromotionForUse(Promotion promotion, BigDecimal orderValue) {

        if (promotion.isDeleted()) {
            throw new AppException(ErrorCode.PROMOTION_ALREADY_DELETED);
        }

        LocalDate now = LocalDate.now();

        if (now.isBefore(promotion.getStartDate())) {
            throw new AppException(ErrorCode.PROMOTION_NOT_STARTED);
        }

        if (now.isAfter(promotion.getEndDate())) {
            throw new AppException(ErrorCode.PROMOTION_EXPIRED);
        }

        if (promotion.getQuantity() <= promotion.getUsedQuantity()) {
            throw new AppException(ErrorCode.PROMOTION_OUT_OF_STOCK);
        }

        if (promotion.getMinOrderValue() != null &&
                orderValue.compareTo(promotion.getMinOrderValue()) < 0) {
            throw new AppException(ErrorCode.MIN_ORDER_VALUE_INVALID);
        }
    }

    // Helper method: Lấy số lần user đã sử dụng promotion
    private int getUserUsageCount(String userId, String promotionId) {
        Integer usageCount = usedPromotionRepository.getTotalUsageCountByUserAndPromotion(userId, promotionId);
        return usageCount != null ? usageCount : 0;
    }


    private BigDecimal calculateDiscount(Promotion promotion, BigDecimal orderValue) {
        if (promotion.getDiscountType() == DiscountType.PERCENTAGE) {
            BigDecimal discount = orderValue
                    .multiply(promotion.getDiscountValue())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            if (promotion.getMaxDiscount() != null &&
                    discount.compareTo(promotion.getMaxDiscount()) > 0) {
                discount = promotion.getMaxDiscount();
            }
            return discount;
        } else {
            return promotion.getDiscountValue();
        }
    }
}
