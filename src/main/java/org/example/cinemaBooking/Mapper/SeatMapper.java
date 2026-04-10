package org.example.cinemaBooking.Mapper;

import org.example.cinemaBooking.DTO.Request.Seat.UpdateSeatRequest;
import org.example.cinemaBooking.DTO.Response.Seat.SeatResponse;
import org.example.cinemaBooking.Entity.Seat;

import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface SeatMapper {

    @Mapping(target = "roomId", source = "room.id")
    @Mapping(target = "seatTypeId", source = "seatType.id")
    @Mapping(target = "seatTypeName", source = "seatType.name")
    @Mapping(target = "priceModifier", source = "seatType.priceModifier")
    SeatResponse toResponse(Seat seat);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateSeat(@MappingTarget Seat seat, UpdateSeatRequest updateSeatRequest);
}