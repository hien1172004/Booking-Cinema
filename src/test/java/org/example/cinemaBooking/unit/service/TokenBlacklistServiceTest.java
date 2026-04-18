package org.example.cinemaBooking.unit.service;

import org.example.cinemaBooking.Service.redis.TokenBlacklistService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
public class TokenBlacklistServiceTest {
    @InjectMocks
    private TokenBlacklistService tokenBlacklistService;

    @Test
    void testBlacklistToken() {
        // TODO: Thêm kiểm thử cho blacklist token
    }
}
