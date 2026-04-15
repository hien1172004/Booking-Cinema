package org.example.cinemaBooking.Service.Chatbot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
@Slf4j
@Service
public class ChatbotService {

    private static final String SYSTEM_PROMPT = """
            Bạn là trợ lý ảo thông minh của Cinema Booking System - hệ thống đặt vé xem phim trực tuyến.
            
            Nhiệm vụ của bạn:
            - Tư vấn và giới thiệu các bộ phim đang chiếu và sắp chiếu.
            - Cung cấp lịch chiếu, thông tin rạp và giờ chiếu chính xác.
            - Gợi ý phim phù hợp theo sở thích thể loại hoặc diễn viên.
            - Hỗ trợ khách tra cứu trạng thái đơn đặt vé bằng mã booking.
            - Tra cứu suất chiếu theo phim + ngày hoặc theo rạp + ngày.
            - Tra cứu bảng giá vé và phụ phí loại ghế (thường, VIP, đôi).
            - Kiểm tra ghế trống theo suất chiếu cụ thể.
            - Tra cứu phim đang chiếu tại một rạp cụ thể.
            - Giới thiệu combo bắp nước, đồ ăn và đồ uống.
            - Cung cấp thông tin chương trình khuyến mãi, mã giảm giá.
            - Trả lời các câu hỏi chung về điện ảnh với kiến thức của bạn.
            
            Quy tắc QUAN TRỌNG:
            - Luôn sử dụng Tool phù hợp để lấy dữ liệu thực tế. Đừng bịa thông tin.
            - Khi khách hỏi về ngày cụ thể, hãy truyền đúng ngày vào tool (dd/MM/yyyy).
              Nếu khách nói "hôm nay" hãy dùng ngày hiện tại, "ngày mai" tính thêm 1 ngày, v.v.
            - Nếu khách muốn ĐẶT VÉ, hãy hướng dẫn họ thực hiện trực tiếp trên website/app của chúng tôi.
              Bạn KHÔNG thể thực hiện đặt vé, thanh toán hoặc hủy vé.
            - Luôn trả lời bằng tiếng Việt, thân thiện, ngắn gọn và dễ hiểu.
            - Nếu không biết thông tin, hãy thành thật nói không biết và gợi ý khách liên hệ hotline.
            - Sử dụng các tool được cung cấp để lấy thông tin chính xác từ hệ thống thay vì tự đoán.
            """;

    private static final int WINDOW_SIZE = 10;

    private final ChatClient chatClient;

    public ChatbotService(ChatClient.Builder chatClientBuilder, ChatbotTools chatbotTools, JdbcChatMemoryRepository jdbcChatMemoryRepository) {


        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(jdbcChatMemoryRepository)
                .maxMessages(WINDOW_SIZE )
                .build();

        chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(chatbotTools)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .defaultOptions(
                        GoogleGenAiChatOptions.builder()
                                .temperature(0.7)
                                .maxOutputTokens(500)
                                .build()
                )
                .build();
    }

    public String chat(String userMessage) {
        String conversationId = getConversationId();
        log.info("[CHATBOT] conversationId={} | message={}", conversationId, userMessage);

        String reply = chatClient.prompt()
                .user(userMessage)
                .advisors(advisorSpec -> advisorSpec
                        .param(ChatMemory.CONVERSATION_ID, conversationId)
                )
                .call()
                .content();
        if (reply == null || reply.isBlank()) {
            reply = "Xin lỗi, hiện tại tôi chưa thể trả lời. Bạn thử lại sau nhé!";
        }
        log.info("[CHATBOT] conversationId={} | reply length={}", conversationId, reply != null ? reply.length() : 0);
        return reply;
    }

    private String getConversationId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "anonymous";
        }
        return "chatbot:" + auth.getName();
    }
}