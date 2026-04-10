package org.example.cinemaBooking.Entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.cinemaBooking.Shared.persistence.SoftDeletableEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Combo extends SoftDeletableEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(length = 500)
    private String image;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @OneToMany(mappedBy = "combo", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ComboItem> items = new ArrayList<>();


    /**
     * Thêm item vào combo
     */
    public void addItem(ComboItem item) {
        items.add(item);
        item.setCombo(this);
    }

    /**
     * Xóa item khỏi combo
     */
    public void removeItem(ComboItem item) {
        items.remove(item);
        item.setCombo(null);
    }
    /**
     * Tính tổng giá trị các items trong combo
     */
    public BigDecimal calculateItemsTotal() {
        return items.stream()
                .map(item -> item.getProduct().getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Kiểm tra combo có lợi hơn mua lẻ không
     */
    public boolean isDiscounted() {
        return price.compareTo(calculateItemsTotal()) < 0;
    }

    /**
     * Tính phần trăm giảm giá
     */
    public BigDecimal getDiscountPercentage() {
        BigDecimal total = calculateItemsTotal();
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return total.subtract(price)
                .divide(total, 2, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
}
