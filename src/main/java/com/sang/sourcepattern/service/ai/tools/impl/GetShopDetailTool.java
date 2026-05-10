package com.sang.sourcepattern.service.ai.tools.impl;

import com.sang.sourcepattern.dto.response.ServiceResponse;
import com.sang.sourcepattern.dto.response.ShopResponse;
import com.sang.sourcepattern.entity.Service;
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
public class GetShopDetailTool implements AITool {

    private final ShopService shopService;
    private final ServiceRepository serviceRepository;

    @Override
    public String getName() { return "get_shop_detail"; }

    @Override
    public Set<String> getSupportedAgents() { return Set.of("USER_CHAT"); }

    @Override
    public Map<String, Object> getSchema() {
        return Map.of(
                "name", "get_shop_detail",
                "description", "Lấy thông tin chi tiết của một shop cụ thể bao gồm dịch vụ, giá cả, đánh giá",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "shopId", Map.of("type", "number", "description", "ID của shop")
                        ),
                        "required", List.of("shopId")
                )
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> args, Jwt jwt) {
        int shopId = ((Number) args.get("shopId")).intValue();
        ShopResponse shop = shopService.getVerifiedShopById(shopId);

        List<ServiceResponse> services = serviceRepository.findByShopIdAndActiveTrue(shopId)
                .stream()
                .map(s -> ServiceResponse.builder()
                        .id(s.getId()).shopId(shopId).shopName(shop.getShopName())
                        .serviceName(s.getServiceName()).category(s.getCategory())
                        .price(s.getPrice()).durationMinutes(s.getDurationMinutes())
                        .description(s.getDescription()).active(s.isActive())
                        .build())
                .collect(Collectors.toList());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("shop", shop);
        data.put("services", services);

        Map<String, Object> summary = Map.of(
                "id", shop.getId(), "name", shop.getShopName(),
                "rating", shop.getRatingAvg(), "city", shop.getCity(),
                "serviceCount", services.size(),
                "services", services.stream().map(s -> Map.of(
                        "id", s.getId(), "name", s.getServiceName(), "price", s.getPrice()
                )).collect(Collectors.toList())
        );

        return ToolResult.builder()
                .type("shop_detail")
                .data(data)
                .geminiSummary(summary)
                .build();
    }
}
