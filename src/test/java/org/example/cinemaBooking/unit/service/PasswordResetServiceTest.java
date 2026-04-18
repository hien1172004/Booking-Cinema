package org.example.cinemaBooking.unit.service;

import org.example.cinemaBooking.Service.Auth.PasswordResetService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
public class PasswordResetServiceTest {
    @InjectMocks
    private PasswordResetService passwordResetService;

    @Test
    void testResetPassword() {
        // TODO: Thêm kiểm thử cho reset password
    }
}
