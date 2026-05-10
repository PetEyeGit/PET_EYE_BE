package com.sang.sourcepattern.controller;

import com.sang.sourcepattern.dto.request.AIChatSaveRequest;
import com.sang.sourcepattern.dto.response.AIChatHistoryResponse;
import com.sang.sourcepattern.dto.response.ApiResponse;
import com.sang.sourcepattern.entity.Shop;
import com.sang.sourcepattern.entity.ShopAIChatHistory;
import com.sang.sourcepattern.entity.User;
import com.sang.sourcepattern.exception.AppException;
import com.sang.sourcepattern.exception.ErrorCode;
import com.sang.sourcepattern.repository.ShopAIChatHistoryRepository;
import com.sang.sourcepattern.repository.ShopRepository;
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
@RequestMapping("/shop-ai")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Shop AI Assistant", description = "Lưu và lấy lịch sử chat với PetEye Business AI (Shop Owner)")
public class ShopAIController {

    ShopAIChatHistoryRepository historyRepo;
    ShopRepository shopRepository;
    UserRepository userRepository;

    // ─── GET history ──────────────────────────────────────────────────────────

    @GetMapping("/history")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @Operation(summary = "Lấy toàn bộ lịch sử chat AI của shop")
    public ApiResponse<List<AIChatHistoryResponse>> getHistory(@AuthenticationPrincipal Jwt jwt) {
        int shopId = resolveShopId(jwt);
        List<AIChatHistoryResponse> history = historyRepo
                .findByShopIdOrderByCreatedAtAsc(shopId)
                .stream().map(this::toResponse).toList();
        return ApiResponse.<List<AIChatHistoryResponse>>builder().result(history).build();
    }

    // ─── POST save 1 message ──────────────────────────────────────────────────

    @PostMapping("/history")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @Operation(summary = "Lưu 1 tin nhắn vào lịch sử chat AI")
    public ApiResponse<AIChatHistoryResponse> saveMessage(
            @RequestBody @Valid AIChatSaveRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        int shopId = resolveShopId(jwt);
        ShopAIChatHistory msg = ShopAIChatHistory.builder()
                .shopId(shopId)
                .role(request.getRole())
                .content(request.getContent())
                .build();
        msg = historyRepo.save(msg);
        return ApiResponse.<AIChatHistoryResponse>builder().result(toResponse(msg)).build();
    }

    // ─── POST batch save ──────────────────────────────────────────────────────

    @PostMapping("/history/batch")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @Operation(summary = "Lưu nhiều tin nhắn cùng lúc")
    public ApiResponse<List<AIChatHistoryResponse>> saveMessages(
            @RequestBody @Valid List<AIChatSaveRequest> requests,
            @AuthenticationPrincipal Jwt jwt) {

        int shopId = resolveShopId(jwt);
        List<ShopAIChatHistory> entities = requests.stream()
                .map(r -> ShopAIChatHistory.builder()
                        .shopId(shopId).role(r.getRole()).content(r.getContent()).build())
                .toList();
        List<AIChatHistoryResponse> saved = historyRepo.saveAll(entities)
                .stream().map(this::toResponse).toList();
        return ApiResponse.<List<AIChatHistoryResponse>>builder().result(saved).build();
    }

    // ─── DELETE clear ─────────────────────────────────────────────────────────

    @DeleteMapping("/history")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @Operation(summary = "Xoá toàn bộ lịch sử chat AI của shop")
    public ApiResponse<Void> clearHistory(@AuthenticationPrincipal Jwt jwt) {
        int shopId = resolveShopId(jwt);
        historyRepo.deleteByShopId(shopId);
        return ApiResponse.<Void>builder().message("Đã xoá lịch sử chat AI").build();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private int resolveShopId(Jwt jwt) {
        String email = jwt.getClaim("email");
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        Shop shop = shopRepository.findByOwnerId(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));
        return shop.getId();
    }

    private AIChatHistoryResponse toResponse(ShopAIChatHistory h) {
        return AIChatHistoryResponse.builder()
                .id(h.getId()).role(h.getRole())
                .content(h.getContent()).createdAt(h.getCreatedAt()).build();
    }
}
