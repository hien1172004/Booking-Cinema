package org.example.cinemaBooking.DTO.Request.Chatbot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Yêu cầu gửi tin nhắn đến chatbot.
 *
 * @param message Nội dung tin nhắn của người dùng
 */
public record ChatRequest(
        @NotBlank(message = "Message must not be blank")
        @Size(max = 2000, message = "Message must not exceed 2000 characters")
        String message
) {}
