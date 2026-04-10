package org.example.cinemaBooking.Mapper;

import org.example.cinemaBooking.DTO.Response.Movie.MovieCastResponse;
import org.example.cinemaBooking.DTO.Response.Movie.MoviePeopleResponse;

import org.example.cinemaBooking.Entity.MoviePeople;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MoviePeopleMapper {
    @Mapping(source = "people.id", target = "peopleId")
    @Mapping(source = "people.name", target = "name")
    @Mapping(source = "people.avatarUrl", target = "avatarUrl")
    MovieCastResponse toMovieCastResponse(MoviePeople moviePeople);

    @Mapping(source = "movie.id", target = "movieId")
    @Mapping(source = "movie.title", target = "movieTitle")
    @Mapping(source = "people.id", target = "peopleId")
    @Mapping(source = "people.name", target = "peopleName")
    @Mapping(source = "people.avatarUrl", target = "peopleAvatar")
    @Mapping(source = "movieRole", target = "role")
    MoviePeopleResponse toMoviePeopleResponse(MoviePeople moviePeople);

}
