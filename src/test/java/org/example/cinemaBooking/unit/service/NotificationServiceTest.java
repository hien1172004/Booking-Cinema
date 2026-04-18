package org.example.cinemaBooking.unit.service;

import org.example.cinemaBooking.Service.Notification.NotificationService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {
    @InjectMocks
    private NotificationService notificationService;

    @Test
    void testSendNotification() {
        // TODO: Thêm kiểm thử cho gửi notification
    }
}
