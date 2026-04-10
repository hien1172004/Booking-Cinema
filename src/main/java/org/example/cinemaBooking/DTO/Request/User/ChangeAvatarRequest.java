package org.example.cinemaBooking.DTO.Request.User;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class ChangeAvatarRequest {
    @NotBlank(message = "IMAGE_URL_NOT_BLANK")
    String avatarUrl;
}
