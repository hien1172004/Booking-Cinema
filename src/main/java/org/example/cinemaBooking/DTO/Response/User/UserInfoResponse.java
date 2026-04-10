package org.example.cinemaBooking.DTO.Response.User;

import lombok.Data;

@Data
public class UserInfoResponse {
    String username;
    String fullName;
    String email;
    String phone;
    boolean status;

}
