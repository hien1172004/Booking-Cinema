package org.example.cinemaBooking.DTO.Response.Chatbot;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Phản hồi từ chatbot trả về client.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatResponse {

    /** Nội dung phản hồi của AI */
    String reply;
}
