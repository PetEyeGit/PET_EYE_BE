package com.sang.sourcepattern.controller;

import com.sang.sourcepattern.dto.request.ChatHistorySaveRequest;
import com.sang.sourcepattern.dto.response.ApiResponse;
import com.sang.sourcepattern.dto.response.ChatHistoryResponse;
import com.sang.sourcepattern.entity.ChatHistory;
import com.sang.sourcepattern.entity.User;
import com.sang.sourcepattern.exception.AppException;
import com.sang.sourcepattern.exception.ErrorCode;
import com.sang.sourcepattern.repository.ChatHistoryRepository;
import com.sang.sourcepattern.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chatbot")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Chatbot", description = "Lưu và lấy lịch sử chat với PetEye Assistant")
public class ChatbotController {

    ChatHistoryRepository chatHistoryRepository;
    UserRepository userRepository;

    // ─── Lấy lịch sử chat ────────────────────────────────────────────────────

    @GetMapping("/history")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Lấy toàn bộ lịch sử chat của user hiện tại")
    public ApiResponse<List<ChatHistoryResponse>> getHistory(@AuthenticationPrincipal Jwt jwt) {
        int userId = resolveUserId(jwt);
        List<ChatHistoryResponse> history = chatHistoryRepository
                .findByUserIdOrderByCreatedAtAsc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
        return ApiResponse.<List<ChatHistoryResponse>>builder().result(history).build();
    }

    // ─── Lưu 1 tin nhắn ──────────────────────────────────────────────────────

    @PostMapping("/history")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Lưu 1 tin nhắn vào lịch sử chat")
    public ApiResponse<ChatHistoryResponse> saveMessage(
            @RequestBody @Valid ChatHistorySaveRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        int userId = resolveUserId(jwt);

        ChatHistory msg = ChatHistory.builder()
                .userId(userId)
                .role(request.getRole())
                .content(request.getContent())
                .toolResultJson(request.getToolResultJson())
                .build();

        msg = chatHistoryRepository.save(msg);
        return ApiResponse.<ChatHistoryResponse>builder().result(toResponse(msg)).build();
    }

    // ─── Lưu nhiều tin nhắn cùng lúc (batch) ─────────────────────────────────

    @PostMapping("/history/batch")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Lưu nhiều tin nhắn cùng lúc")
    public ApiResponse<List<ChatHistoryResponse>> saveMessages(
            @RequestBody @Valid List<ChatHistorySaveRequest> requests,
            @AuthenticationPrincipal Jwt jwt) {

        int userId = resolveUserId(jwt);

        List<ChatHistory> entities = requests.stream()
                .map(req -> ChatHistory.builder()
                        .userId(userId)
                        .role(req.getRole())
                        .content(req.getContent())
                        .toolResultJson(req.getToolResultJson())
                        .build())
                .toList();

        List<ChatHistoryResponse> saved = chatHistoryRepository.saveAll(entities)
                .stream().map(this::toResponse).toList();

        return ApiResponse.<List<ChatHistoryResponse>>builder().result(saved).build();
    }

    // ─── Xoá lịch sử ─────────────────────────────────────────────────────────

    @DeleteMapping("/history")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Xoá toàn bộ lịch sử chat của user")
    public ApiResponse<Void> clearHistory(@AuthenticationPrincipal Jwt jwt) {
        int userId = resolveUserId(jwt);
        chatHistoryRepository.deleteByUserId(userId);
        return ApiResponse.<Void>builder().message("Đã xoá lịch sử chat").build();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /** Lấy userId từ JWT claim "email" */
    private int resolveUserId(Jwt jwt) {
        String email = jwt.getClaim("email");
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        return user.getId();
    }

    private ChatHistoryResponse toResponse(ChatHistory c) {
        return ChatHistoryResponse.builder()
                .id(c.getId())
                .role(c.getRole())
                .content(c.getContent())
                .toolResultJson(c.getToolResultJson())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
