package org.example.cinemaBooking.DTO.Response.Auth;

import lombok.Builder;
import lombok.Data;

@Builder
@Data

public class RefreshResponse {
    Boolean success;
    String accessToken;
}
