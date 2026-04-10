package org.example.cinemaBooking.Config;

import lombok.RequiredArgsConstructor;
import org.example.cinemaBooking.Service.AI.CinemaChatTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.ai", name = "enabled", havingValue = "true")
public class CinemaChatConfig {

    private final CinemaChatTools cinemaTools;
    private final ChatMemory chatMemory; // 🔥 inject Redis memory

    @Bean
    public ChatClient cinemaChatClient(ChatClient.Builder builder) {

        return builder
                .defaultSystem("""
                    Bạn là trợ lý AI của hệ thống đặt vé phim CinemaBooking.

                    Vai trò:
                    - Tìm phim đang chiếu / sắp chiếu
                    - Cung cấp thông tin phim (nội dung, thời lượng, thể loại)
                    - Hiển thị lịch chiếu và sơ đồ ghế
                    - Gợi ý phim phù hợp
                    - Hỗ trợ quy trình đặt vé

                    Nguyên tắc bắt buộc:
                    - Luôn trả lời tiếng Việt, ngắn gọn, rõ ràng
                    - KHÔNG tự bịa dữ liệu
                    - CHỈ dùng dữ liệu từ tools
                    - Nếu không có dữ liệu → nói rõ "không tìm thấy"
                    - Nếu cần dữ liệu → PHẢI gọi tool trước khi trả lời
                    - Không tự đoán suất chiếu hoặc ghế

                    Format:
                    - Giờ chiếu: HH:mm
                    - Nội dung rõ ràng

                    Ngày hiện tại: {{current_date}}
                """)
                .defaultTools(cinemaTools)
                // 🔥 QUAN TRỌNG NHẤT
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(chatMemory)
                )
                .build();
    }
}