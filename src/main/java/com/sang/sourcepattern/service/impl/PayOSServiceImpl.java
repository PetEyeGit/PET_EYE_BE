package com.sang.sourcepattern.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sang.sourcepattern.dto.request.PayOSCreateRequest;
import com.sang.sourcepattern.dto.response.PayOSCreateResponse;
import com.sang.sourcepattern.dto.response.PayOSPaymentInfoResponse;
import com.sang.sourcepattern.service.PayOSService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;

@Service
@Slf4j
public class PayOSServiceImpl implements PayOSService {

    private static final String PAYOS_BASE_URL = "https://api-merchant.payos.vn";

    @Value("${payos.client-id}")
    private String clientId;

    @Value("${payos.api-key}")
    private String apiKey;

    @Value("${payos.checksum-key}")
    private String checksumKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ─── Signature ────────────────────────────────────────────────────────────

    /**
     * PayOS signature = HMAC_SHA256 of sorted data string.
     * Format: "amount=X&cancelUrl=X&description=X&orderCode=X&returnUrl=X"
     * (sorted alphabetically by key)
     */
    private String createSignature(Long orderCode, int amount, String description,
                                   String returnUrl, String cancelUrl) {
        try {
            String data = "amount=" + amount
                    + "&cancelUrl=" + cancelUrl
                    + "&description=" + description
                    + "&orderCode=" + orderCode
                    + "&returnUrl=" + returnUrl;

            log.info("PayOS signature data: [{}]", data);

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    checksumKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create PayOS signature", e);
        }
    }

    // ─── Create payment link ──────────────────────────────────────────────────

    @Override
    public PayOSCreateResponse createPaymentLink(Long orderCode, int amount,
                                                  String description,
                                                  String returnUrl, String cancelUrl) {
        String signature = createSignature(orderCode, amount, description, returnUrl, cancelUrl);

        PayOSCreateRequest request = PayOSCreateRequest.builder()
                .orderCode(orderCode)
                .amount(amount)
                .description(description)
                .items(List.of(
                        PayOSCreateRequest.PayOSItem.builder()
                                .name(description)
                                .quantity(1)
                                .price(amount)
                                .build()
                ))
                .returnUrl(returnUrl)
                .cancelUrl(cancelUrl)
                .signature(signature)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-client-id", clientId);
        headers.set("x-api-key", apiKey);

        HttpEntity<PayOSCreateRequest> entity = new HttpEntity<>(request, headers);

        log.info("=== PayOS HTTP Request ===");
        log.info("URL         : {}/v2/payment-requests", PAYOS_BASE_URL);
        log.info("orderCode   : {}", orderCode);
        log.info("amount      : {}", amount);
        log.info("description : {}", description);
        log.info("signature   : {}", signature);
        log.info("=========================");

        try {
            ResponseEntity<PayOSCreateResponse> response = restTemplate.exchange(
                    PAYOS_BASE_URL + "/v2/payment-requests",
                    HttpMethod.POST,
                    entity,
                    PayOSCreateResponse.class
            );

            PayOSCreateResponse body = response.getBody();
            log.info("PayOS response: code={}, desc={}", body != null ? body.getCode() : "null",
                    body != null ? body.getDesc() : "null");

            if (body != null && body.getData() != null) {
                log.info("checkoutUrl : {}", body.getData().getCheckoutUrl());
            }

            return body;
        } catch (Exception e) {
            log.error("PayOS HTTP call failed: {}", e.getMessage(), e);
            throw new RuntimeException("PayOS createPaymentLink failed: " + e.getMessage(), e);
        }
    }

    // ─── Get payment info ─────────────────────────────────────────────────────

    @Override
    public PayOSPaymentInfoResponse getPaymentInfo(Long orderCode) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-client-id", clientId);
        headers.set("x-api-key", apiKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<PayOSPaymentInfoResponse> response = restTemplate.exchange(
                    PAYOS_BASE_URL + "/v2/payment-requests/" + orderCode,
                    HttpMethod.GET,
                    entity,
                    PayOSPaymentInfoResponse.class
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("PayOS getPaymentInfo failed for orderCode={}: {}", orderCode, e.getMessage());
            throw new RuntimeException("PayOS getPaymentInfo failed: " + e.getMessage(), e);
        }
    }
}
