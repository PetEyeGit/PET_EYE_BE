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
                "description", "Tự động tìm shop, dịch vụ, thú cưng phù hợp rồi hiển thị form đặt lịch. Dùng ngay khi user muốn đặt lịch với tên shop + dịch vụ + tên thú cưng.",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "shopName", Map.of("type", "string",
                                        "description", "Tên shop (một phần cũng được)"),
                                "serviceKeyword", Map.of("type", "string",
                                        "description", "Tên dịch vụ cần đặt, ví dụ: tắm, cắt lông"),
                                "petName", Map.of("type", "string",
                                        "description", "Tên thú cưng (một phần cũng được)")
                        ),
                        "required", List.of("serviceKeyword")
                )
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> args, Jwt jwt) {
        String email = jwt.getClaim("email");
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        String shopNameQuery = args.get("shopName") instanceof String
                ? ((String) args.get("shopName")).toLowerCase().trim() : "";
        String serviceKw = ((String) args.get("serviceKeyword")).toLowerCase().trim();
        String petNameQuery = args.get("petName") instanceof String
                ? ((String) args.get("petName")).toLowerCase().trim() : "";

        // 1. Find shop
        List<ShopResponse> shops = shopService.searchVerifiedShops(
                shopNameQuery.isEmpty() ? null : shopNameQuery, null, null);

        if (shops.isEmpty()) {
            return ToolResult.error("Không tìm thấy shop: " + args.get("shopName"));
        }

        ShopResponse matchedShop = shopNameQuery.isEmpty() ? shops.get(0) :
                shops.stream()
                        .filter(s -> s.getShopName().toLowerCase().contains(shopNameQuery))
                        .findFirst()
                        .orElse(shops.get(0));

        // 2. Find service in that shop
        List<Service> shopServices = serviceRepository.findByShopIdAndActiveTrue(matchedShop.getId());
        Service matchedService = shopServices.stream()
                .filter(s -> s.getServiceName().toLowerCase().contains(serviceKw)
                        || s.getCategory().toLowerCase().contains(serviceKw)
                        || (s.getDescription() != null && s.getDescription().toLowerCase().contains(serviceKw)))
                .findFirst()
                .orElse(null);

        if (matchedService == null) {
            return ToolResult.error("Shop \"" + matchedShop.getShopName()
                    + "\" không có dịch vụ \"" + args.get("serviceKeyword") + "\"");
        }

        // 3. Find pet
        List<PetResponse> pets = petService.getPetsByOwner(user.getId()).stream()
                .filter(PetResponse::isActive)
                .collect(Collectors.toList());

        if (pets.isEmpty()) {
            return ToolResult.error("Bạn chưa có thú cưng. Vui lòng thêm thú cưng trong hồ sơ.");
        }

        PetResponse matchedPet = petNameQuery.isEmpty() ? pets.get(0) :
                pets.stream()
                        .filter(p -> p.getName().toLowerCase().contains(petNameQuery)
                                || p.getSpecies().toLowerCase().contains(petNameQuery))
                        .findFirst()
                        .orElse(pets.get(0));

        // 4. Return booking_picker data
        Map<String, Object> pickerData = new LinkedHashMap<>();
        pickerData.put("shopId", matchedShop.getId());
        pickerData.put("shopName", matchedShop.getShopName());
        pickerData.put("serviceId", matchedService.getId());
        pickerData.put("serviceName", matchedService.getServiceName());
        pickerData.put("servicePrice", matchedService.getPrice());
        pickerData.put("petId", matchedPet.getId());
        pickerData.put("petName", matchedPet.getName());

        return ToolResult.builder()
                .type("booking_picker")
                .data(pickerData)
                .geminiSummary(Map.of(
                        "ready", true,
                        "shopName", matchedShop.getShopName(),
                        "serviceName", matchedService.getServiceName(),
                        "petName", matchedPet.getName(),
                        "message", "Đã tìm thấy đầy đủ thông tin, hiển thị form chọn ngày giờ"
                ))
                .build();
    }
}
