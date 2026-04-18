package org.example.cinemaBooking.unit.service;

import org.example.cinemaBooking.Service.RateLimit.RateLimitService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
public class RateLimitServiceTest {
    @InjectMocks
    private RateLimitService rateLimitService;

    @Test
    void testCheckRateLimit() {
        // TODO: Thêm kiểm thử cho kiểm tra rate limit
    }
}
