package org.example.cinemaBooking.Controller.AI;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.ai", name = "enabled", havingValue = "true")
public class CinemaChatController {

    private final ChatClient cinemaChatClient;

    private static final String SESSION_KEY = "X-Session-Id";

    // =========================
    // STREAMING CHAT (SSE)
    // =========================
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
        public ResponseEntity<Flux<ServerSentEvent<String>>> streamChat(
            @Valid @RequestBody ChatRequest request,
            @RequestHeader(value = SESSION_KEY, required = false) String sessionId) {

        String sid = resolveSession(sessionId);

        Flux<ServerSentEvent<String>> stream = cinemaChatClient.prompt()
                .user(request.message())
                .system(s -> s.param("current_date", LocalDate.now().toString())) // 🔥 FIX
                .advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, sid))
                .stream()
                .content()
                .map(chunk -> ServerSentEvent.builder(chunk).build())
                .timeout(Duration.ofSeconds(60)) // 🔥 tránh treo
                .onErrorResume(e ->
                    Flux.just(ServerSentEvent.builder(resolveUserMessage(e)).build())
                );

        return ResponseEntity.ok()
            .header(SESSION_KEY, sid)
            .contentType(MediaType.TEXT_EVENT_STREAM)
            .body(stream);
    }

    // =========================
    // NON-STREAMING CHAT
    // =========================
    @PostMapping
        public ResponseEntity<ChatResponse> chat(
            @Valid @RequestBody ChatRequest request,
            @RequestHeader(value = SESSION_KEY, required = false) String sessionId) {

        String sid = resolveSession(sessionId);

        try {
            String response = cinemaChatClient.prompt()
                .user(request.message())
                .system(s -> s.param("current_date", LocalDate.now().toString())) // 🔥 FIX
                .advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, sid))
                .call()
                .content();

            return ResponseEntity.ok()
                .header(SESSION_KEY, sid)
                .body(new ChatResponse(response, sid));
        } catch (Exception e) {
            String message = resolveUserMessage(e);
            HttpStatus status = isQuotaOrRateLimitError(e)
                ? HttpStatus.TOO_MANY_REQUESTS
                : HttpStatus.SERVICE_UNAVAILABLE;

            return ResponseEntity.status(status)
                .header(SESSION_KEY, sid)
                .body(new ChatResponse(message, sid));
        }
    }

    // =========================
    // SESSION HANDLER
    // =========================
    private String resolveSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank() || sessionId.equals("anonymous")) {
            return UUID.randomUUID().toString();
        }
        return sessionId;
    }

    private boolean isQuotaOrRateLimitError(Throwable e) {
        String msg = e == null ? "" : String.valueOf(e.getMessage());
        return msg.contains("429") || msg.contains("RESOURCE_EXHAUSTED") || msg.contains("quota");
    }

    private String resolveUserMessage(Throwable e) {
        if (isQuotaOrRateLimitError(e)) {
            return "AI đang vượt hạn mức sử dụng. Vui lòng thử lại sau khoảng 1 phút.";
        }
        return "Hệ thống AI đang bận, vui lòng thử lại sau.";
    }

    // DTO
    public record ChatRequest(@NotBlank(message = "INVALID_REQUEST") String message) {}
    public record ChatResponse(String message, String sessionId) {}
}