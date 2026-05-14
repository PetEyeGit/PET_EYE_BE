package com.sang.sourcepattern.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

/**
 * Geocoding service sử dụng Nominatim (OpenStreetMap) — miễn phí, không cần API key.
 * Tự động convert địa chỉ text → lat/lon khi shop/user đăng ký hoặc cập nhật địa chỉ.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeocodingService {

    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search";
    private static final String USER_AGENT = "PetEyeApp/1.0";

    private final RestTemplate restTemplate;

    /**
     * Geocode địa chỉ thành tọa độ [lat, lon].
     * Thử nhiều cách rút gọn địa chỉ nếu lần đầu thất bại.
     * Trả về null nếu không tìm thấy.
     */
    public double[] geocode(String address) {
        if (address == null || address.isBlank()) return null;

        // Thử lần lượt các biến thể địa chỉ
        String[] candidates = buildCandidates(address);
        for (String candidate : candidates) {
            double[] result = callNominatim(candidate);
            if (result != null) return result;
        }
        log.warn("Geocoding không tìm thấy kết quả cho địa chỉ: {}", address);
        return null;
    }

    /**
     * Tạo danh sách địa chỉ thử theo thứ tự từ chi tiết → đơn giản.
     * Ví dụ: "S7.02 Vinhome Grand Park, Long Bình, Thủ Đức, TP. Hồ Chí Minh, Việt Nam"
     *   → "Long Bình, Thủ Đức, TP. Hồ Chí Minh, Việt Nam"
     *   → "Thủ Đức, TP. Hồ Chí Minh, Việt Nam"
     *   → "TP. Hồ Chí Minh, Việt Nam"
     */
    private String[] buildCandidates(String address) {
        // Chuẩn hóa: bỏ dấu chấm trong "TP." → "TP", "Q." → "Quan"
        String normalized = address
                .replace("TP. Hồ Chí Minh", "Ho Chi Minh City")
                .replace("TP.HCM", "Ho Chi Minh City")
                .replace("TP.Hồ Chí Minh", "Ho Chi Minh City")
                .replace("Hồ Chí Minh", "Ho Chi Minh City")
                .replace("Thủ Đức", "Thu Duc")
                .replace("Bình Thạnh", "Binh Thanh")
                .replace("Phú Nhuận", "Phu Nhuan")
                .replace("Tân Bình", "Tan Binh")
                .replace("Quận ", "District ")
                .replace("Q.", "District ");

        String[] parts = normalized.split(",");
        java.util.List<String> candidates = new java.util.ArrayList<>();

        // Thêm từ đầy đủ → rút gọn dần
        for (int i = 0; i < parts.length; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = i; j < parts.length; j++) {
                if (sb.length() > 0) sb.append(",");
                sb.append(parts[j].trim());
            }
            String candidate = sb.toString().trim();
            if (!candidate.isBlank()) candidates.add(candidate);
        }

        return candidates.toArray(new String[0]);
    }

    private double[] callNominatim(String query) {
        try {
            String url = UriComponentsBuilder.fromUriString(NOMINATIM_URL)
                    .queryParam("q", query)
                    .queryParam("format", "json")
                    .queryParam("limit", 1)
                    .queryParam("countrycodes", "vn")
                    .build()
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", USER_AGENT);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity,
                    new ParameterizedTypeReference<>() {}
            );

            List<Map<String, Object>> results = response.getBody();
            if (results == null || results.isEmpty()) return null;

            Map<String, Object> first = results.get(0);
            double lat = Double.parseDouble(first.get("lat").toString());
            double lon = Double.parseDouble(first.get("lon").toString());
            log.info("Geocoded '{}' → [{}, {}]", query, lat, lon);
            return new double[]{lat, lon};

        } catch (Exception e) {
            log.error("Nominatim call failed for '{}': {}", query, e.getMessage());
            return null;
        }
    }
}
