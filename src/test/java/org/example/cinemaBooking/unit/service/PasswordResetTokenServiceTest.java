package org.example.cinemaBooking.unit.service;

import org.example.cinemaBooking.Service.redis.PasswordResetTokenService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
public class PasswordResetTokenServiceTest {
    @InjectMocks
    private PasswordResetTokenService passwordResetTokenService;

    @Test
    void testCreateToken() {
        // TODO: Thêm kiểm thử cho tạo token
    }
}
