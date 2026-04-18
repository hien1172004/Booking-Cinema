package org.example.cinemaBooking.unit.service;

import org.example.cinemaBooking.Service.Cloudinary.CloudinaryService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
public class CloudinaryServiceTest {
    @InjectMocks
    private CloudinaryService cloudinaryService;

    @Test
    void testUploadImage() {
        // TODO: Thêm kiểm thử cho upload ảnh
    }
}
