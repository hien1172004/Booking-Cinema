package org.example.cinemaBooking.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.cinemaBooking.DTO.Request.Chatbot.ChatRequest;
import org.example.cinemaBooking.DTO.Response.Chatbot.ChatResponse;
import org.example.cinemaBooking.Service.Chatbot.ChatbotService;
import org.example.cinemaBooking.Shared.constant.ApiPaths;
import org.example.cinemaBooking.Shared.constraints.RateLimit;
import org.example.cinemaBooking.Shared.response.ApiResponse;
import org.springframework.web.bind.annotation.*;

/**
 * Controller cung cấp endpoint giao tiếp với Chatbot AI.
 * Yêu cầu người dùng đã đăng nhập (JWT).
 */
@RestController
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping(ApiPaths.API_V1 + ApiPaths.Chatbot.BASE)
@Tag(name = "Chatbot", description = "Trợ lý ảo hỗ trợ khách hàng (yêu cầu đăng nhập)")
public class ChatbotController {

    ChatbotService chatbotService;

    @Operation(
            summary = "Gửi tin nhắn đến Chatbot",
            description = """
                    Gửi tin nhắn đến AI Chatbot và nhận phản hồi thông minh.
                    Chatbot có thể:
                    - Gợi ý phim hay, phim theo thể loại, phim theo diễn viên
                    - Tra cứu lịch chiếu phim hôm nay
                    - Kiểm tra trạng thái đơn đặt vé
                    - Cung cấp thông tin liên hệ rạp
                    
                    Yêu cầu: Bearer JWT token hợp lệ.
                    """
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping(ApiPaths.Chatbot.CHAT)
    @RateLimit(capacity = 20, refillPerMinute = 20)
    public ApiResponse<ChatResponse> chat(@RequestBody @Valid ChatRequest request) {
        String reply = chatbotService.chat(request.message());
        return ApiResponse.<ChatResponse>builder()
                .success(true)
                .data(ChatResponse.builder().reply(reply).build())
                .build();
    }
}
