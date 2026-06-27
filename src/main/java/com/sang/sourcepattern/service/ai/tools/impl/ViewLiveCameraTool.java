package com.sang.sourcepattern.service.ai.tools.impl;

import com.sang.sourcepattern.entity.Booking;
import com.sang.sourcepattern.entity.User;
import com.sang.sourcepattern.exception.AppException;
import com.sang.sourcepattern.exception.ErrorCode;
import com.sang.sourcepattern.repository.BookingRepository;
import com.sang.sourcepattern.repository.UserRepository;
import com.sang.sourcepattern.service.ai.tools.AITool;
import com.sang.sourcepattern.service.ai.tools.ToolResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ViewLiveCameraTool implements AITool {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    @Override
    public String getName() {
        return "view_live_camera";
    }

    @Override
    public Set<String> getSupportedAgents() {
        return Set.of("USER_CHAT");
    }

    @Override
    public Map<String, Object> getSchema() {
        return Map.of(
                "name", "view_live_camera",
                "description", "Lấy luồng Live Camera (HLS) của thú cưng đang gửi lưu trú. Dùng khi user muốn xem hình ảnh, video, hoặc xem thú cưng đang làm gì.",
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

        Booking activeCameraBooking = null;

        for (Booking b : userBookings) {
            if (b.getPet() != null && b.getPet().getName().toLowerCase().contains(petNameQuery)) {
                if (b.getCameraStreamUrl() != null && !b.getCameraStreamUrl().isEmpty()) {
                    if ("IN_PROGRESS".equals(b.getStatus()) || "CONFIRMED".equals(b.getStatus())) {
                        activeCameraBooking = b;
                        break;
                    }
                }
            }
        }

        if (activeCameraBooking == null) {
            return ToolResult.error("Không tìm thấy luồng Camera nào đang hoạt động cho bé " + args.get("petName") + ". Có thể bé chưa check-in hoặc gói dịch vụ không bao gồm Camera.");
        }

        return ToolResult.builder()
                .type("camera_view")
                .data(Map.of(
                        "petName", activeCameraBooking.getPet().getName(),
                        "shopName", activeCameraBooking.getShop().getShopName(),
                        "streamUrl", activeCameraBooking.getCameraStreamUrl()
                ))
                .geminiSummary(Map.of(
                        "petName", activeCameraBooking.getPet().getName(),
                        "shopName", activeCameraBooking.getShop().getShopName(),
                        "message", "Đã tìm thấy Camera. Giao diện Live Camera và hình ảnh (Snapshot) tự động đã được hiển thị trên màn hình của user."
                ))
                .build();
    }
}
