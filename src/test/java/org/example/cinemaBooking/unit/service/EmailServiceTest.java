package org.example.cinemaBooking.unit.service;

import org.example.cinemaBooking.Service.Auth.EmailService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
public class EmailServiceTest {
    @InjectMocks
    private EmailService emailService;

    @Test
    void testSendEmail() {
        // TODO: Thêm kiểm thử cho gửi email
    }
}
