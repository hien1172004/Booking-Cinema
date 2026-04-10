package org.example.cinemaBooking.Entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.cinemaBooking.Shared.persistence.SoftDeletableEntity;
import org.example.cinemaBooking.Shared.enums.ItemType;

import java.math.BigDecimal;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingProduct extends SoftDeletableEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    // ── Discriminator ────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 10)
    private ItemType itemType;   // PRODUCT hoặc COMBO

    @Column(name = "item_id", nullable = false)
    private String itemId;         // trỏ vào product.id hoặc combo.id tùy itemType


    @Column(nullable = false)
    private String itemName;     // tên sp/combo lúc đặt

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal itemPrice; // đơn giá lúc đặt

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;

//    @Column(nullable = false, precision = 10, scale = 2)
//    private BigDecimal totalPrice; // itemPrice * quantity

//    // ── Factory methods ──────────────────────────────────────────────
//    public static BookingProduct fromProduct(Booking booking, Product product, int qty) {
//        BigDecimal unitPrice = product.getPrice();
//        return BookingProduct.builder()
//                .booking(booking)
//                .itemType(ItemType.PRODUCT)
//                .itemId(product.getId())
//                .itemName(product.getName())
//                .itemPrice(unitPrice)
//                .quantity(qty)
//                .totalPrice(unitPrice.multiply(BigDecimal.valueOf(qty)))
//                .build();
//    }
//
//    public static BookingProduct fromCombo(Booking booking, Combo combo, int qty) {
//        BigDecimal unitPrice = combo.getPrice();
//        return BookingProduct.builder()
//                .booking(booking)
//                .itemType(ItemType.COMBO)
//                .itemId(combo.getId())
//                .itemName(combo.getName())
//                .itemPrice(unitPrice)
//                .quantity(qty)
//                .totalPrice(unitPrice.multiply(BigDecimal.valueOf(qty)))
//                .build();
//    }

    public boolean isProduct() { return itemType == ItemType.PRODUCT; }
    public boolean isCombo()   { return itemType == ItemType.COMBO; }
}
