package com.sang.sourcepattern.service.ai.tools.impl;

import com.sang.sourcepattern.dto.request.BookingCreationRequest;
import com.sang.sourcepattern.dto.response.BookingResponse;
import com.sang.sourcepattern.dto.response.PetResponse;
import com.sang.sourcepattern.dto.response.ShopResponse;
import com.sang.sourcepattern.entity.Service;
import com.sang.sourcepattern.entity.User;
import com.sang.sourcepattern.exception.AppException;
import com.sang.sourcepattern.exception.ErrorCode;
import com.sang.sourcepattern.repository.ServiceRepository;
import com.sang.sourcepattern.repository.UserRepository;
import com.sang.sourcepattern.service.BookingService;
import com.sang.sourcepattern.service.PetService;
import com.sang.sourcepattern.service.ShopService;
import com.sang.sourcepattern.service.ai.tools.AITool;
import com.sang.sourcepattern.service.ai.tools.ToolResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

@Component
@RequiredArgsConstructor
public class CreateBookingTool implements AITool {

    private final BookingService bookingService;
    private final ShopService shopService;
    private final PetService petService;
    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;

    @Override
    public String getName() { return "create_booking"; }

    @Override
    public Set<String> getSupportedAgents() { return Set.of("USER_CHAT"); }

    @Override
    public Map<String, Object> getSchema() {
        return Map.of(
                "name", "create_booking",
                "description", "Tạo booking sau khi đã có đủ shopId, serviceId, petId VÀ appointmentDatetime. Chỉ gọi khi user đã chọn ngày giờ cụ thể.",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "shopId", Map.of("type", "number", "description", "ID shop"),
                                "serviceId", Map.of("type", "number", "description", "ID dịch vụ"),
                                "petId", Map.of("type", "number", "description", "ID thú cưng"),
                                "appointmentDatetime", Map.of("type", "string",
                                        "description", "Ngày giờ hẹn ISO: 2025-06-15T10:00:00"),
                                "note", Map.of("type", "string", "description", "Ghi chú (tuỳ chọn)")
                        ),
                        "required", List.of("shopId", "serviceId", "petId", "appointmentDatetime")
                )
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> args, Jwt jwt) {
        String email = jwt.getClaim("email");
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        int shopId = ((Number) args.get("shopId")).intValue();
        int serviceId = ((Number) args.get("serviceId")).intValue();
        int petId = ((Number) args.get("petId")).intValue();
        String datetimeStr = (String) args.get("appointmentDatetime");
        String note = (String) args.get("note");

        // If no datetime → return booking_picker
        if (datetimeStr == null || datetimeStr.isBlank()) {
            ShopResponse shop = shopService.getVerifiedShopById(shopId);
            Service service = serviceRepository.findById(serviceId)
                    .orElseThrow(() -> new AppException(ErrorCode.SERVICE_NOT_FOUND));
            PetResponse pet = petService.getPet(petId);

            Map<String, Object> pickerData = new LinkedHashMap<>();
            pickerData.put("shopId", shopId);
            pickerData.put("shopName", shop.getShopName());
            pickerData.put("serviceId", serviceId);
            pickerData.put("serviceName", service.getServiceName());
            pickerData.put("servicePrice", service.getPrice());
            pickerData.put("petId", petId);
            pickerData.put("petName", pet.getName());

            return ToolResult.builder()
                    .type("booking_picker")
                    .data(pickerData)
                    .geminiSummary(Map.of("needDatetime", true,
                            "shopName", shop.getShopName(),
                            "serviceName", service.getServiceName()))
                    .build();
        }

        // Create cash booking — initiate 10% deposit via PayOS
        BookingCreationRequest request = BookingCreationRequest.builder()
                .shopId(shopId)
                .serviceId(serviceId)
                .petId(petId)
                .appointmentDatetime(LocalDateTime.parse(datetimeStr))
                .note(note)
                .paymentMethod("CASH")
                .build();

        com.sang.sourcepattern.dto.response.InitiatePaymentResponse depositResponse =
                bookingService.initiateCashDeposit(request, email);

        Map<String, Object> successData = new LinkedHashMap<>();
        successData.put("orderCode", depositResponse.getOrderCode());
        successData.put("checkoutUrl", depositResponse.getCheckoutUrl());
        successData.put("depositAmount", depositResponse.getAmount());
        successData.put("shopId", shopId);
        successData.put("serviceId", serviceId);
        successData.put("petId", petId);
        successData.put("datetime", datetimeStr);
        successData.put("message", "Vui lòng thanh toán tiền cọc 10% qua link bên dưới để xác nhận lịch hẹn.");

        return ToolResult.builder()
                .type("cash_deposit_required")
                .data(successData)
                .geminiSummary(Map.of(
                        "success", true,
                        "requiresDeposit", true,
                        "depositAmount", depositResponse.getAmount(),
                        "checkoutUrl", depositResponse.getCheckoutUrl(),
                        "datetime", datetimeStr
                ))
                .build();
    }
}
