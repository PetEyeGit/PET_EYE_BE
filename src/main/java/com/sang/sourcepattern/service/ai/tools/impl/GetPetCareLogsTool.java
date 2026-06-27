package com.sang.sourcepattern.service.ai.tools.impl;

import com.sang.sourcepattern.entity.Booking;
import com.sang.sourcepattern.entity.CareLog;
import com.sang.sourcepattern.entity.User;
import com.sang.sourcepattern.exception.AppException;
import com.sang.sourcepattern.exception.ErrorCode;
import com.sang.sourcepattern.repository.BookingRepository;
import com.sang.sourcepattern.repository.CareLogRepository;
import com.sang.sourcepattern.repository.UserRepository;
import com.sang.sourcepattern.service.ai.tools.AITool;
import com.sang.sourcepattern.service.ai.tools.ToolResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class GetPetCareLogsTool implements AITool {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final CareLogRepository careLogRepository;

    @Override
    public String getName() {
        return "get_pet_care_logs";
    }

    @Override
    public Set<String> getSupportedAgents() {
        return Set.of("USER_CHAT");
    }

    @Override
    public Map<String, Object> getSchema() {
        return Map.of(
                "name", "get_pet_care_logs",
                "description", "Lấy nhật ký chăm sóc (log), lịch trình các hoạt động (ăn uống, tắm, chơi...) của thú cưng đang gửi lưu trú.",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "petName", Map.of("type", "string", "description", "Tên thú cưng")
                        ),
                        "required", List.of("petName")
                )
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> args, Jwt jwt) {
        String email = jwt.getClaim("email");
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        String petNameQuery = args.get("petName") instanceof String
                ? ((String) args.get("petName")).toLowerCase().trim() : "";

        List<Booking> userBookings = bookingRepository.findByUserIdWithServices(user.getId());
        userBookings.sort((b1, b2) -> Integer.compare(b2.getId(), b1.getId()));

        Booking activeBooking = null;

        for (Booking b : userBookings) {
            if (b.getPet() != null && b.getPet().getName().toLowerCase().contains(petNameQuery)) {
                if ("IN_PROGRESS".equals(b.getStatus()) || "CONFIRMED".equals(b.getStatus()) || "COMPLETED".equals(b.getStatus())) {
                    activeBooking = b;
                    break;
                }
            }
        }

        if (activeBooking == null) {
            return ToolResult.error("Không tìm thấy thú cưng nào tên " + args.get("petName") + " đang được gửi tại Shop.");
        }

        List<CareLog> logs = careLogRepository.findByBookingIdOrderByTimestampDesc(activeBooking.getId());
        
        if (logs.isEmpty()) {
            return ToolResult.builder()
                    .type("text_only")
                    .geminiSummary(Map.of(
                            "message", "Hiện tại chưa có nhật ký hoạt động nào được ghi lại cho bé " + activeBooking.getPet().getName() + " tại " + activeBooking.getShop().getShopName() + "."
                    ))
                    .build();
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
        String logsText = logs.stream()
                .map(log -> String.format("- [%s]: %s (%s)", 
                        log.getTimestamp().format(formatter), 
                        log.getType(), 
                        log.getNote() != null ? log.getNote() : "Không có ghi chú"))
                .collect(Collectors.joining("\n"));

        return ToolResult.builder()
                .type("text_only")
                .geminiSummary(Map.of(
                        "petName", activeBooking.getPet().getName(),
                        "shopName", activeBooking.getShop().getShopName(),
                        "logs", logsText,
                        "instruction", "Hãy đọc danh sách nhật ký trên và tóm tắt lại bằng giọng điệu thân thiện cho người dùng, KHÔNG HIỂN THỊ DẠNG JSON. Không cần gọi tool camera nữa. Ví dụ: 'Dạ bé Mochi tại Dev Pet Spa đã được cho ăn uống nước lúc 05:13...'"
                ))
                .build();
    }
}
