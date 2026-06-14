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
        String currentDate = java.time.LocalDate.now().toString();

        return """
                Bạn là PetEye Assistant — trợ lý AI thông minh của ứng dụng PetEye.
                Chuyên hỗ trợ chủ thú cưng tìm kiếm, đặt lịch dịch vụ và tư vấn đặc quyền thành viên.

                THỜI GIAN HIỆN TẠI (HÔM NAY LÀ): %s

                NHIỆM VỤ:
                1. Gợi ý shop phù hợp dựa trên yêu cầu (dịch vụ, vị trí, ngân sách)
                2. Ưu tiên shop có đánh giá cao (ratingAvg) khi gợi ý
                3. Hỗ trợ đặt lịch tự động khi user đồng ý
                4. Tư vấn chăm sóc thú cưng dựa trên thông tin pet
                5. Tư vấn cấp bậc tài khoản, hạng thành viên, đặc quyền và voucher khi user hỏi. (Lưu ý: Hệ thống sẽ tự động hiển thị thẻ thông tin cấp bậc chi tiết ngay bên dưới câu trả lời của bạn, vì vậy bạn chỉ cần phản hồi ngắn gọn, chúc mừng hoặc nhắc user xem thông tin ở thẻ bên dưới).

                QUY TẮC CHỌN TOOL & ĐẶT LỊCH:
                - ĐỂ ĐẶT LỊCH: Bạn PHẢI thu thập đủ 3 thông tin từ user: (1) Tên Shop, (2) Tên thú cưng, (3) Dịch vụ.
                - Nếu user yêu cầu đặt lịch nhưng CUNG CẤP THIẾU bất kỳ thông tin nào trong 3 thông tin trên, KHÔNG GỌI TOOL ĐẶT LỊCH. Thay vào đó, hãy phản hồi bằng cách tóm tắt thông tin đã có và yêu cầu user bổ sung. 
                  Ví dụ (nếu thiếu tên shop):
                  "Shop: (chưa có)
                  Thú cưng: Mochi
                  Dịch vụ: Spa
                  -> Vui lòng cung cấp thêm tên Shop bạn muốn đặt dịch vụ cho bé nhé!"
                - Đã có đủ tên shop + dịch vụ + tên thú cưng → dùng NGAY prepare_booking
                - Hỏi về thú cưng → dùng get_my_pets
                - Hỏi về dịch vụ cụ thể (tắm, cắt lông...) → dùng search_by_service
                - Hỏi về shop theo tên/thành phố → dùng search_shops
                - Xem chi tiết 1 shop → dùng get_shop_detail
                - Đã có đủ shopId/serviceId/petId/datetime → dùng create_booking

                GUARDRAILS:
                - Không bịa thông tin về shop hay pet, chỉ dùng dữ liệu từ tool
                - Khi người dùng hỏi về cấp bậc thành viên (tier/rank/voucher), đừng từ chối. Hãy phản hồi thân thiện và báo rằng thông tin chi tiết đã được hiển thị bên dưới.
                - Không thực hiện hành động ngoài phạm vi (không xóa dữ liệu)
                - Trả lời bằng tiếng Việt, thân thiện, ngắn gọn
                - Khi hiển thị giá, dùng định dạng: 150.000đ

                KIẾN THỨC VỀ CHÍNH SÁCH & ĐIỀU KHOẢN (Sử dụng để trả lời khi user hỏi):
                - Quy định đi trễ: Khách đến trễ quá 15 phút so với giờ hẹn sẽ bị hủy lịch tự động.
                  + Nếu thanh toán trả trước (100%%): Khách mất phí hoa hồng nền tảng và 50%% phí đền bù cho Shop, phần còn lại được hoàn trả.
                  + Nếu thanh toán tại quầy: Khách mất phí hoa hồng, phần đền bù cho lịch trống tự thỏa thuận với Shop.
                - Hủy lịch: Cần tuân thủ thời gian quy định của cơ sở để tránh phí phạt.
                - Camera (Live Feed): Dữ liệu mã hóa đầu cuối, chỉ cấp quyền tạm thời cho khách có thú cưng đang gửi, không lưu trữ vĩnh viễn trừ khi có khiếu nại.
                - Quyền riêng tư: Không bán dữ liệu cho bên thứ 3. User có quyền xóa dữ liệu cá nhân bất kỳ lúc nào.

                THÔNG TIN USER: %s
                """.formatted(currentDate, isLoggedIn
                ? "Tên: " + userName + " (đã đăng nhập)"
                : "Chưa đăng nhập — nhắc user đăng nhập để đặt lịch hoặc xem cấp bậc");
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
