package com.sang.sourcepattern.service.ai.tools.impl;

import com.sang.sourcepattern.dto.response.PetResponse;
import com.sang.sourcepattern.dto.response.ShopResponse;
import com.sang.sourcepattern.entity.Service;
import com.sang.sourcepattern.entity.User;
import com.sang.sourcepattern.exception.AppException;
import com.sang.sourcepattern.exception.ErrorCode;
import com.sang.sourcepattern.repository.ServiceRepository;
import com.sang.sourcepattern.repository.UserRepository;
import com.sang.sourcepattern.service.PetService;
import com.sang.sourcepattern.service.ShopService;
import com.sang.sourcepattern.service.ai.tools.AITool;
import com.sang.sourcepattern.service.ai.tools.ToolResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PrepareBookingTool implements AITool {

    private final ShopService shopService;
    private final PetService petService;
    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;

    @Override
    public String getName() { return "prepare_booking"; }

    @Override
    public Set<String> getSupportedAgents() { return Set.of("USER_CHAT"); }

    @Override
    public Map<String, Object> getSchema() {
        return Map.of(
                "name", "prepare_booking",
                "description", "Tự động tìm shop, dịch vụ, thú cưng phù hợp rồi hiển thị form đặt lịch. Hỗ trợ đặt lịch cho NHIỀU thú cưng/dịch vụ cùng lúc.",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "bookings", Map.of(
                                        "type", "array",
                                        "description", "Danh sách các yêu cầu đặt lịch",
                                        "items", Map.of(
                                                "type", "object",
                                                "properties", Map.of(
                                                        "shopName", Map.of("type", "string", "description", "Tên shop (một phần cũng được)"),
                                                        "serviceKeyword", Map.of("type", "string", "description", "Tên dịch vụ cần đặt"),
                                                        "petName", Map.of("type", "string", "description", "Tên thú cưng"),
                                                        "appointmentDate", Map.of("type", "string", "description", "Ngày hẹn YYYY-MM-DD"),
                                                        "appointmentTime", Map.of("type", "string", "description", "Giờ hẹn HH:mm"),
                                                        "appointmentEndDate", Map.of("type", "string", "description", "Ngày kết thúc"),
                                                        "appointmentEndTime", Map.of("type", "string", "description", "Giờ kết thúc"),
                                                        "note", Map.of("type", "string", "description", "Ghi chú cho cơ sở")
                                                ),
                                                "required", List.of("serviceKeyword", "petName")
                                        )
                                )
                        ),
                        "required", List.of("bookings")
                )
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> args, Jwt jwt) {
        String email = jwt.getClaim("email");
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        List<Map<String, Object>> bookings = (List<Map<String, Object>>) args.get("bookings");
        if (bookings == null || bookings.isEmpty()) {
            if (args.containsKey("serviceKeyword")) {
                bookings = List.of(args);
            } else {
                return ToolResult.error("Không có thông tin đặt lịch.");
            }
        }

        List<Map<String, Object>> resultDataList = new ArrayList<>();
        List<Map<String, Object>> summaryList = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (Map<String, Object> bookingArgs : bookings) {
            String shopNameQuery = bookingArgs.get("shopName") instanceof String
                    ? ((String) bookingArgs.get("shopName")).toLowerCase().trim() : "";
            String serviceKw = bookingArgs.get("serviceKeyword") instanceof String
                    ? ((String) bookingArgs.get("serviceKeyword")).toLowerCase().trim() : "";
            String petNameQuery = bookingArgs.get("petName") instanceof String
                    ? ((String) bookingArgs.get("petName")).toLowerCase().trim() : "";
            String appDate = bookingArgs.get("appointmentDate") instanceof String ? (String) bookingArgs.get("appointmentDate") : null;
            String appTime = bookingArgs.get("appointmentTime") instanceof String ? (String) bookingArgs.get("appointmentTime") : null;
            String appEndDate = bookingArgs.get("appointmentEndDate") instanceof String ? (String) bookingArgs.get("appointmentEndDate") : null;
            String appEndTime = bookingArgs.get("appointmentEndTime") instanceof String ? (String) bookingArgs.get("appointmentEndTime") : null;
            String note = bookingArgs.get("note") instanceof String ? (String) bookingArgs.get("note") : null;

            List<ShopResponse> shops = shopService.searchVerifiedShops(
                    shopNameQuery.isEmpty() ? null : shopNameQuery, null, null);

            if (shops.isEmpty()) {
                errors.add("Không tìm thấy shop: " + bookingArgs.get("shopName"));
                continue;
            }

            ShopResponse matchedShop = null;
            Service matchedService = null;

            if (!shopNameQuery.isEmpty()) {
                matchedShop = shops.stream()
                        .filter(s -> s.getShopName().toLowerCase().contains(shopNameQuery))
                        .findFirst()
                        .orElse(shops.get(0));

                List<Service> shopServices = serviceRepository.findByShopIdAndActiveTrue(matchedShop.getId());
                matchedService = shopServices.stream()
                        .filter(s -> s.getServiceName().toLowerCase().contains(serviceKw)
                                || s.getCategory().toLowerCase().contains(serviceKw)
                                || (s.getDescription() != null && s.getDescription().toLowerCase().contains(serviceKw)))
                        .findFirst()
                        .orElse(null);

                if (matchedService == null) {
                    errors.add("Shop \"" + matchedShop.getShopName() + "\" không có dịch vụ \"" + bookingArgs.get("serviceKeyword") + "\"");
                    continue;
                }
            } else {
                for (ShopResponse shop : shops) {
                    List<Service> shopServices = serviceRepository.findByShopIdAndActiveTrue(shop.getId());
                    matchedService = shopServices.stream()
                            .filter(s -> s.getServiceName().toLowerCase().contains(serviceKw)
                                    || s.getCategory().toLowerCase().contains(serviceKw)
                                    || (s.getDescription() != null && s.getDescription().toLowerCase().contains(serviceKw)))
                            .findFirst()
                            .orElse(null);
                    
                    if (matchedService != null) {
                        matchedShop = shop;
                        break;
                    }
                }

                if (matchedShop == null || matchedService == null) {
                    errors.add("Không tìm thấy shop nào có dịch vụ \"" + bookingArgs.get("serviceKeyword") + "\"");
                    continue;
                }
            }

            List<PetResponse> pets = petService.getPetsByOwner(user.getId()).stream()
                    .filter(PetResponse::isActive)
                    .collect(Collectors.toList());

            if (pets.isEmpty()) {
                errors.add("Bạn chưa có thú cưng.");
                continue;
            }

            PetResponse matchedPet = petNameQuery.isEmpty() ? pets.get(0) :
                    pets.stream()
                            .filter(p -> p.getName().toLowerCase().contains(petNameQuery)
                                    || p.getSpecies().toLowerCase().contains(petNameQuery))
                            .findFirst()
                            .orElse(pets.get(0));

            Map<String, Object> pickerData = new LinkedHashMap<>();
            pickerData.put("shopId", matchedShop.getId());
            pickerData.put("shopName", matchedShop.getShopName());
            pickerData.put("serviceId", matchedService.getId());
            pickerData.put("serviceName", matchedService.getServiceName());
            pickerData.put("servicePrice", matchedService.getPrice());
            pickerData.put("petId", matchedPet.getId());
            pickerData.put("petName", matchedPet.getName());
            
            boolean isBoarding = "BOARDING".equalsIgnoreCase(matchedService.getCategory()) 
                              || "HOTEL".equalsIgnoreCase(matchedService.getCategory());
            if (isBoarding) pickerData.put("isBoarding", true);
            
            if (appDate != null) pickerData.put("prefilledDate", appDate);
            if (appTime != null) pickerData.put("prefilledTime", appTime);
            if (appEndDate != null) pickerData.put("prefilledEndDate", appEndDate);
            if (appEndTime != null) pickerData.put("prefilledEndTime", appEndTime);
            if (note != null) pickerData.put("note", note);

            resultDataList.add(pickerData);
            summaryList.add(Map.of(
                    "shopName", matchedShop.getShopName(),
                    "serviceName", matchedService.getServiceName(),
                    "petName", matchedPet.getName()
            ));
        }

        if (resultDataList.isEmpty()) {
            return ToolResult.error("Không thể tạo lịch: " + String.join(", ", errors));
        }

        return ToolResult.builder()
                .type("booking_picker")
                .data(resultDataList.size() == 1 ? resultDataList.get(0) : resultDataList) // Backward compatible for Chatbot.tsx if it doesn't handle array yet
                .geminiSummary(Map.of(
                        "ready", true,
                        "bookings", summaryList,
                        "errors", errors,
                        "message", "Đã hiển thị form đặt lịch cho các yêu cầu."
                ))
                .build();
    }
}
