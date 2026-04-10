package org.example.cinemaBooking.Entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.cinemaBooking.Shared.persistence.SoftDeletableEntity;
import org.example.cinemaBooking.Shared.enums.DiscountType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
@Table(indexes = {
        @Index(name = "idx_promotion_code", columnList = "code"),
        @Index(name = "idx_promotion_date", columnList = "startDate,endDate")
})
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Promotion extends SoftDeletableEntity {
    @Column(nullable = false, unique = true, length = 50)
    String code;
    @Column(nullable = false, length = 50)
    String name;

    @Column(columnDefinition = "TEXT")
    String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    DiscountType discountType;

    @Column(nullable = false, precision = 10, scale = 2)
    BigDecimal discountValue;

    @Builder.Default
    @Column(precision = 10, scale = 2)
    private BigDecimal minOrderValue = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal maxDiscount;  // giới hạn khi dùng PERCENTAGE

    @Column(nullable = false)
    private Integer quantity;   // tổng phát hành

    @Column(nullable = false)
    @Builder.Default
    private Integer usedQuantity = 0; // đã dùng

    @Column(nullable = false)
    LocalDate startDate;

    @Column(nullable = false)
    LocalDate endDate;

    @Column(nullable = false)
    @Builder.Default
    Integer maxUsagePerUser = 1; // giới hạn số lần sử dụng cho mỗi người dùng

    public boolean isActive() {
        LocalDate now = LocalDate.now();
        return !now.isBefore(startDate) && !now.isAfter(endDate) && !this.isDeleted();
    }

    public boolean isAvailable() {
        return usedQuantity < quantity;
    }

    public boolean canApply(BigDecimal orderTotal) {
        if (orderTotal == null) return false;
        return isActive() && isAvailable() && orderTotal.compareTo(minOrderValue) >= 0;
    }

    public BigDecimal calculateDiscount(BigDecimal orderTotal) {
        if (!canApply(orderTotal)) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount;

        if (discountType == DiscountType.FIXED) {
            discount = discountValue.min(orderTotal); // không được vượt quá tổng đơn hàng
        } else {
            discount = orderTotal.multiply(discountValue)
                    .divide(new BigDecimal("100"),2, RoundingMode.HALF_DOWN);
        }

        if (maxDiscount != null) {
            discount = discount.min(maxDiscount);
        }

        return discount;
    }

    public boolean canUserApply(int userUsedCount) {
        return userUsedCount < maxUsagePerUser;
    }

}
