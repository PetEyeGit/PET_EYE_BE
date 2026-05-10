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
public class SearchShopsTool implements AITool {

    private final ShopService shopService;
    private final ServiceRepository serviceRepository;

    @Override
    public String getName() { return "search_shops"; }

    @Override
    public Set<String> getSupportedAgents() { return Set.of("USER_CHAT"); }

    @Override
    public Map<String, Object> getSchema() {
        return Map.of(
                "name", "search_shops",
                "description", "Tìm kiếm các shop thú cưng theo tên, thành phố, loại shop",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "keyword", Map.of("type", "string", "description", "Từ khóa tên shop"),
                                "city", Map.of("type", "string", "description", "Thành phố"),
                                "shopType", Map.of("type", "string",
                                        "description", "Loại: GROOMING, CLINIC, BOARDING, SPA"),
                                "sortByRating", Map.of("type", "boolean",
                                        "description", "Sắp xếp theo đánh giá cao nhất")
                        )
                )
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> args, Jwt jwt) {
        String keyword = (String) args.get("keyword");
        String city = (String) args.get("city");
        String shopType = (String) args.get("shopType");
        boolean sortByRating = Boolean.TRUE.equals(args.get("sortByRating"));

        List<ShopResponse> shops = shopService.searchVerifiedShops(keyword, city, shopType);

        if (sortByRating) {
            shops = shops.stream()
                    .sorted((a, b) -> Float.compare(b.getRatingAvg(), a.getRatingAvg()))
                    .collect(Collectors.toList());
        }

        List<ShopResponse> top5 = shops.stream().limit(5).collect(Collectors.toList());

        // Build shop+services data
        List<Map<String, Object>> shopsWithServices = top5.stream().map(shop -> {
            List<ServiceResponse> services = serviceRepository.findByShopIdAndActiveTrue(shop.getId())
                    .stream()
                    .map(s -> ServiceResponse.builder()
                            .id(s.getId()).shopId(shop.getId()).shopName(shop.getShopName())
                            .serviceName(s.getServiceName()).category(s.getCategory())
                            .price(s.getPrice()).durationMinutes(s.getDurationMinutes())
                            .description(s.getDescription()).imageUrl(s.getImageUrl())
                            .active(s.isActive())
                            .build())
                    .collect(Collectors.toList());

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("shop", shop);
            entry.put("services", services);
            return entry;
        }).collect(Collectors.toList());

        // Gemini summary (compact)
        List<Map<String, Object>> summary = top5.stream().map(s -> Map.<String, Object>of(
                "id", s.getId(), "name", s.getShopName(),
                "rating", s.getRatingAvg(), "city", s.getCity()
        )).collect(Collectors.toList());

        return ToolResult.builder()
                .type("shop_list")
                .data(shopsWithServices)
                .geminiSummary(Map.of("count", top5.size(), "shops", summary))
                .build();
    }
}
