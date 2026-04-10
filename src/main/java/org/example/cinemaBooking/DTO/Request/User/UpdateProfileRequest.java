package org.example.cinemaBooking.DTO.Request.User;

import jakarta.validation.constraints.Email;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.cinemaBooking.Shared.enums.Gender;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateProfileRequest {

    String fullName;

    @Email(message = "EMAIL_INVALID")
    String email;

    String phone;

    LocalDate dob;

    Gender gender;
}