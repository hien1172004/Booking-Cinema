package org.example.cinemaBooking.DTO.Response.Movie;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MovieImageResponse {

    String id;
    String imageUrl;

}