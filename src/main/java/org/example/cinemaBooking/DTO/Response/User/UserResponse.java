package org.example.cinemaBooking.DTO.Response.User;

import lombok.*;
import org.example.cinemaBooking.Shared.enums.Gender;

import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private String id;

    private String username;

    private String fullName;

    private String email;

    private String phone;

    private String avatarUrl;

    private LocalDate dob;

    private Gender gender;

    private boolean status;

    private Set<String> roles;
}