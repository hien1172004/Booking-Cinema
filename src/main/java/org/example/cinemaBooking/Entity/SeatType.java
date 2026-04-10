package org.example.cinemaBooking.Entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.cinemaBooking.Shared.persistence.SoftDeletableEntity;
import org.example.cinemaBooking.Shared.enums.SeatTypeEnum;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SeatType extends SoftDeletableEntity {

    @Column(nullable = false)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    SeatTypeEnum name = SeatTypeEnum.STANDARD;

    @Builder.Default
    @Column(nullable = false, precision = 10, scale = 2)
    BigDecimal priceModifier = BigDecimal.ZERO; // Giá trị thêm vào giá cơ bản của vé
}