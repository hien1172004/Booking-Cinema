package org.example.cinemaBooking.DTO.Request.Seat;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.cinemaBooking.Shared.enums.SeatTypeEnum;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateSeatTypeRequest {
    SeatTypeEnum name;
    BigDecimal priceModifier;
}