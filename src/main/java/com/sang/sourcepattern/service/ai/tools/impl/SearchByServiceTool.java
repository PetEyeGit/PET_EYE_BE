package com.sang.sourcepattern.service.ai.tools.impl;

import com.sang.sourcepattern.dto.response.ServiceResponse;
import com.sang.sourcepattern.dto.response.ShopResponse;
import com.sang.sourcepattern.repository.ServiceRepository;
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
public class SearchByServiceTool implements AITool {

    private final ShopService shopService;
    private final ServiceRepository serviceRepository;

    @Override
    public String getName() { return "search_by_service"; }

    @Override
    public Set<String> getSupportedAgents() { return Set.of("USER_CHAT"); }

    @Override
    public Map<String, Object> getSchema() {
        return Map.of(
                "name", "search_by_service",
                "description", "Tìm kiếm shop theo tên dịch vụ cụ thể (tắm, cắt lông, tiêm phòng, lưu trú, grooming, spa, khám bệnh). Trả về top 5 shop có dịch vụ phù hợp.",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "serviceKeyword", Map.of("type", "string",
                                        "description", "Tên dịch vụ cần tìm, ví dụ: tắm, cắt lông, tiêm phòng"),
                                "city", Map.of("type", "string", "description", "Lọc theo thành phố (tuỳ chọn)"),
                                "topN", Map.of("type", "number", "description", "Số lượng shop trả về, mặc định 5")
                        ),
                        "required", List.of("serviceKeyword")
                )
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> args, Jwt jwt) {
        String serviceKeyword = ((String) args.get("serviceKeyword")).toLowerCase().trim();
        String cityFilter = (String) args.get("city");
        int topN = args.get("topN") instanceof Number ? ((Number) args.get("topN")).intValue() : 5;

        // Get all verified shops (optionally filtered by city)
        List<ShopResponse> allShops = shopService.searchVerifiedShops(null, cityFilter, null);

        // For each shop, find matching services
        List<Map<String, Object>> matched = new ArrayList<>();
        for (ShopResponse shop : allShops) {
            List<com.sang.sourcepattern.entity.Service> shopServices = serviceRepository.findByShopIdAndActiveTrue(shop.getId());
            List<ServiceResponse> matchingServices = shopServices.stream()
                    .filter(s -> s.getServiceName().toLowerCase().contains(serviceKeyword)
                            || (s.getDescription() != null && s.getDescription().toLowerCase().contains(serviceKeyword))
                            || s.getCategory().toLowerCase().contains(serviceKeyword))
                    .map(s -> ServiceResponse.builder()
                            .id(s.getId()).shopId(shop.getId()).shopName(shop.getShopName())
                            .serviceName(s.getServiceName()).category(s.getCategory())
                            .price(s.getPrice()).durationMinutes(s.getDurationMinutes())
                            .description(s.getDescription()).active(s.isActive())
                            .build())
                    .collect(Collectors.toList());

            if (!matchingServices.isEmpty()) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("shop", shop);
                entry.put("services", matchingServices);
                matched.add(entry);
            }
        }

        // Sort by rating, take topN
        matched.sort((a, b) -> {
            float ra = ((ShopResponse) a.get("shop")).getRatingAvg();
            float rb = ((ShopResponse) b.get("shop")).getRatingAvg();
            return Float.compare(rb, ra);
        });
        List<Map<String, Object>> result = matched.stream().limit(topN).collect(Collectors.toList());

        List<Map<String, Object>> summary = result.stream().map(e -> {
            ShopResponse s = (ShopResponse) e.get("shop");
            return Map.<String, Object>of("id", s.getId(), "name", s.getShopName(),
                    "rating", s.getRatingAvg(), "city", s.getCity());
        }).collect(Collectors.toList());

        return ToolResult.builder()
                .type("shop_list")
                .data(result)
                .geminiSummary(Map.of("serviceKeyword", serviceKeyword,
                        "count", result.size(), "shops", summary))
                .build();
    }
}
