package org.example.cinemaBooking.DTO.Request.Auth;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class RefreshRequest {
    @NotBlank(message = "TOKEN_REQUIRED")
    String token;
}
