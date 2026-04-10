// ShowtimeSeatMapper.java
package org.example.cinemaBooking.Mapper;


import org.example.cinemaBooking.DTO.Response.Showtime.ShowtimeSeatResponse;
import org.example.cinemaBooking.Entity.ShowtimeSeat;
import org.mapstruct.*;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface ShowtimeSeatMapper {

    /**
     * Entity → Response.
     * finalPrice = showtime.basePrice + seat.seatType.priceModifier
     */
    @Mapping(target = "showtimeSeatId", source = "id")
    @Mapping(target = "seatId",         source = "seat.id")
    @Mapping(target = "seatRow",        source = "seat.seatRow")
    @Mapping(target = "seatNumber",     source = "seat.seatNumber")
    @Mapping(target = "seatType",       source = "seat.seatType.name")
    @Mapping(target = "finalPrice",     expression = "java(calcFinalPrice(ss))")
    @Mapping(target = "status",         source = "status")
    @Mapping(target = "lockedUntil",    source = "lockedUntil")
    @Mapping(target = "lockedByUser",   source = "lockedByUser")
    ShowtimeSeatResponse toResponse(ShowtimeSeat ss);

    // helper — được gọi trong expression ở trên
    default BigDecimal calcFinalPrice(ShowtimeSeat ss) {
        return ss.getShowtime().getBasePrice()
                .add(ss.getSeat().getSeatType().getPriceModifier());
    }
}