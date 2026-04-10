package org.example.cinemaBooking.DTO.Request.Auth;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class LogoutRequest {
    String token;
}
