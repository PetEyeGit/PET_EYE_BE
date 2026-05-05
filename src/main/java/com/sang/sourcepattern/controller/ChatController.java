package com.sang.sourcepattern.controller;

import com.sang.sourcepattern.config.WebSocketAuthInterceptor.WsPrincipal;
import com.sang.sourcepattern.dto.request.ChatMessageRequest;
import com.sang.sourcepattern.dto.response.ApiResponse;
import com.sang.sourcepattern.dto.response.ChatMessageResponse;
import com.sang.sourcepattern.entity.Message;
import com.sang.sourcepattern.repository.MessageRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChatController {

    MessageRepository messageRepository;
    SimpMessagingTemplate messagingTemplate;

    /**
     * WebSocket — FE gửi tới /app/chat
     * Principal được set bởi WebSocketAuthInterceptor khi CONNECT
     */
    @MessageMapping("/chat")
    public void handleMessage(@Payload ChatMessageRequest request, Principal principal) {
        if (principal == null) return;

        WsPrincipal wsPrincipal = (WsPrincipal) principal;
        String senderEmail = wsPrincipal.email();
        String senderRole = wsPrincipal.roles().contains("ADMIN") ? "ADMIN" : "SHOP_OWNER";

        Message message = messageRepository.save(Message.builder()
                .shopId(request.getShopId())
                .senderEmail(senderEmail)
                .senderRole(senderRole)
                .content(request.getContent())
                .build());

        messagingTemplate.convertAndSend("/topic/chat/" + request.getShopId(), toResponse(message));
    }

    /** REST — lấy lịch sử chat */
    @GetMapping("/chat/{shopId}/history")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SHOP_OWNER')")
    public ApiResponse<List<ChatMessageResponse>> getHistory(@PathVariable int shopId) {
        return ApiResponse.<List<ChatMessageResponse>>builder()
                .result(messageRepository.findByShopIdOrderByCreatedAtAsc(shopId)
                        .stream().map(this::toResponse).toList())
                .build();
    }

    /** REST — đánh dấu đã đọc */
    @PatchMapping("/chat/{shopId}/read")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SHOP_OWNER')")
    public ApiResponse<Void> markRead(@PathVariable int shopId,
                                      @AuthenticationPrincipal Jwt jwt) {
        List<String> roles = jwt.getClaim("roles");
        String readerRole = roles.contains("ADMIN") ? "ADMIN" : "SHOP_OWNER";

        messageRepository.findByShopIdOrderByCreatedAtAsc(shopId).stream()
                .filter(m -> !m.getSenderRole().equals(readerRole) && !m.isRead())
                .forEach(m -> {
                    m.setRead(true);
                    messageRepository.save(m);
                });

        return ApiResponse.<Void>builder().message("Marked as read").build();
    }

    private ChatMessageResponse toResponse(Message m) {
        return ChatMessageResponse.builder()
                .id(m.getId())
                .shopId(m.getShopId())
                .senderEmail(m.getSenderEmail())
                .senderRole(m.getSenderRole())
                .content(m.getContent())
                .createdAt(m.getCreatedAt())
                .isRead(m.isRead())
                .build();
    }
}
