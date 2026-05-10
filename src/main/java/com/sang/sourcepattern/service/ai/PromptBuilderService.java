package com.sang.sourcepattern.service.ai;

import com.sang.sourcepattern.service.ai.provider.AIRequest;
import com.sang.sourcepattern.service.ai.provider.FunctionCall;
import com.sang.sourcepattern.service.ai.tools.ToolRegistry;
import com.sang.sourcepattern.service.ai.tools.ToolResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PromptBuilderService {

    private final ToolRegistry toolRegistry;

    public AIRequest build(String agentType, String userMessage,
                           List<Map<String, String>> history,
                           String businessContext, Jwt jwt) {

        String systemPrompt = buildSystemPrompt(agentType, businessContext, jwt);

        // Build messages list: history + current user message
        List<Map<String, String>> messages = new ArrayList<>(history);
        messages.add(Map.of("role", "user", "content", userMessage));

        // Only USER_CHAT uses function calling
        List<Map<String, Object>> tools = "USER_CHAT".equals(agentType)
                ? toolRegistry.getSchemas("USER_CHAT")
                : List.of();

        return AIRequest.builder()
                .systemPrompt(systemPrompt)
                .messages(messages)
                .tools(tools)
                .build();
    }

    /**
     * Build a second AIRequest after tool execution.
     * Appends the model's functionCall turn + user's functionResponse turn.
     */
    public AIRequest buildWithToolResult(AIRequest original, FunctionCall fc, ToolResult toolResult) {
        List<Map<String, String>> messages = new ArrayList<>(original.getMessages());

        // Gemini expects: model turn with functionCall, then user turn with functionResponse
        // We encode these as text messages since we're using the simple REST API
        // The functionResponse summary is injected as a user message
        String toolResponseText = "[Tool: " + fc.getName() + " result]\n"
                + mapToString(toolResult.getGeminiSummary());

        messages.add(Map.of("role", "assistant",
                "content", "[Calling tool: " + fc.getName() + "]"));
        messages.add(Map.of("role", "user", "content", toolResponseText));

        return AIRequest.builder()
                .systemPrompt(original.getSystemPrompt())
                .messages(messages)
                .tools(original.getTools())
                .build();
    }

    // ── System prompts ────────────────────────────────────────────────────────

    private String buildSystemPrompt(String agentType, String businessContext, Jwt jwt) {
        return switch (agentType) {
            case "USER_CHAT" -> buildUserChatPrompt(jwt);
            case "SHOP_ASSISTANT" -> buildShopPrompt(businessContext);
            case "ADMIN_ASSISTANT" -> buildAdminPrompt(businessContext);
            default -> "Bạn là trợ lý AI của PetEye. Trả lời bằng tiếng Việt.";
        };
    }

    private String buildUserChatPrompt(Jwt jwt) {
        String userName = jwt.getClaim("name");
        String userId = jwt.getClaim("sub");
        boolean isLoggedIn = userId != null;

        return """
                Bạn là PetEye Assistant — trợ lý AI thông minh của ứng dụng PetEye.
                Chuyên hỗ trợ chủ thú cưng tìm kiếm và đặt lịch dịch vụ chăm sóc thú cưng.

                NHIỆM VỤ:
                1. Gợi ý shop phù hợp dựa trên yêu cầu (dịch vụ, vị trí, ngân sách)
                2. Ưu tiên shop có đánh giá cao (ratingAvg) khi gợi ý
                3. Hỗ trợ đặt lịch tự động khi user đồng ý
                4. Tư vấn chăm sóc thú cưng dựa trên thông tin pet

                QUY TẮC CHỌN TOOL:
                - Muốn ĐẶT LỊCH với tên shop + dịch vụ + tên thú cưng → dùng NGAY prepare_booking
                - Hỏi về thú cưng → dùng get_my_pets
                - Hỏi về dịch vụ cụ thể (tắm, cắt lông...) → dùng search_by_service
                - Hỏi về shop theo tên/thành phố → dùng search_shops
                - Xem chi tiết 1 shop → dùng get_shop_detail
                - Đã có đủ shopId/serviceId/petId/datetime → dùng create_booking

                GUARDRAILS:
                - Không bịa thông tin về shop hay pet, chỉ dùng dữ liệu từ tool
                - Không thực hiện hành động ngoài phạm vi (không xóa dữ liệu)
                - Trả lời bằng tiếng Việt, thân thiện, ngắn gọn
                - Khi hiển thị giá, dùng định dạng: 150.000đ

                THÔNG TIN USER: %s
                """.formatted(isLoggedIn
                ? "Tên: " + userName + " (đã đăng nhập)"
                : "Chưa đăng nhập — nhắc user đăng nhập để đặt lịch");
    }

    private String buildShopPrompt(String businessContext) {
        return """
                Bạn là PetEye Business AI — trợ lý phân tích kinh doanh cho chủ shop thú cưng.

                NHIỆM VỤ:
                - Phân tích dữ liệu kinh doanh thực tế của shop
                - Đưa ra nhận xét cụ thể dựa trên số liệu
                - Đề xuất hành động có thể thực hiện ngay
                - Phát hiện rủi ro và cơ hội tăng trưởng

                GUARDRAILS:
                - Chỉ phân tích dữ liệu được cung cấp, không bịa số liệu
                - Không tiết lộ thông tin của shop khác
                - Trả lời bằng tiếng Việt, có cấu trúc rõ ràng, dùng emoji phù hợp

                %s
                """.formatted(businessContext);
    }

    private String buildAdminPrompt(String businessContext) {
        return """
                Bạn là PetEye Admin AI — trợ lý quản trị hệ thống thông minh.

                NHIỆM VỤ:
                - Phân tích dữ liệu toàn nền tảng PetEye
                - Phát hiện rủi ro, bất thường, vi phạm
                - Đề xuất chính sách và chiến lược tăng trưởng
                - Hỗ trợ ra quyết định quản trị

                GUARDRAILS:
                - Chỉ phân tích dữ liệu được cung cấp
                - Không tiết lộ thông tin cá nhân cụ thể của user
                - Ưu tiên xử lý vấn đề khẩn cấp trước
                - Trả lời bằng tiếng Việt, có cấu trúc rõ ràng

                %s
                """.formatted(businessContext);
    }

    private String mapToString(Map<String, Object> map) {
        if (map == null) return "{}";
        StringBuilder sb = new StringBuilder();
        map.forEach((k, v) -> sb.append(k).append(": ").append(v).append("\n"));
        return sb.toString();
    }
}
