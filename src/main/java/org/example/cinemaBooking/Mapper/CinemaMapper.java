package org.example.cinemaBooking.Mapper;

import org.example.cinemaBooking.DTO.Request.Cinema.CreateCinemaRequest;
import org.example.cinemaBooking.DTO.Request.Cinema.UpdateCinemaRequest;
import org.example.cinemaBooking.DTO.Response.Cinema.CinemaResponse;
import org.example.cinemaBooking.Entity.Cinema;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface CinemaMapper {

    Cinema toCinema(CreateCinemaRequest request);

    CinemaResponse toResponse(Cinema cinema);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateCinema(UpdateCinemaRequest request, @MappingTarget Cinema cinema);
}