package org.example.cinemaBooking.Mapper;

import org.example.cinemaBooking.DTO.Request.Movie.UpdatePeopleRequest;
import org.example.cinemaBooking.DTO.Response.Movie.PeopleResponse;

import org.example.cinemaBooking.DTO.Request.Movie.CreatePeopleRequest;
import org.example.cinemaBooking.Entity.People;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface PeopleMapper {
    People toEntity(CreatePeopleRequest createPeopleRequest);

    PeopleResponse toResponse(People people);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    People updatePeople(UpdatePeopleRequest request, @MappingTarget People people);


}