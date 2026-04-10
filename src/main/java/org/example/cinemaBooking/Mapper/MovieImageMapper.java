package org.example.cinemaBooking.Mapper;


import org.example.cinemaBooking.DTO.Response.Movie.MovieImageResponse;
import org.example.cinemaBooking.Entity.MovieImage;
import org.mapstruct.Mapper;


@Mapper(componentModel = "spring")
public interface MovieImageMapper {

    MovieImageResponse toResponse(MovieImage movieImage);

}