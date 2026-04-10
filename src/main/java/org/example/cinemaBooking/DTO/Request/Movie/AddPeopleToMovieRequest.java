package org.example.cinemaBooking.DTO.Request.Movie;


import java.util.List;

public record AddPeopleToMovieRequest(
        List<PeopleRoleRequest> people
){
}