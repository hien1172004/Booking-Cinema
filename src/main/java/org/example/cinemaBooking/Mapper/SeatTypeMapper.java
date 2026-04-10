package org.example.cinemaBooking.Mapper;

import org.example.cinemaBooking.DTO.Response.Seat.SeatTypeResponse;
import org.example.cinemaBooking.Entity.SeatType;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SeatTypeMapper {

    @Mapping(target = "name", expression = "java(seatType.getName().name())")
    SeatTypeResponse toResponse(SeatType seatType);
}