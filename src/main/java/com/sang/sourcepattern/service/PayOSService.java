package com.sang.sourcepattern.service;

import com.sang.sourcepattern.dto.response.PayOSCreateResponse;
import com.sang.sourcepattern.dto.response.PayOSPaymentInfoResponse;

public interface PayOSService {

    /**
     * Create a PayOS payment link via direct HTTP call.
     * @return PayOSCreateResponse containing checkoutUrl and qrCode
     */
    PayOSCreateResponse createPaymentLink(
            Long orderCode,
            int amount,
            String description,
            String returnUrl,
            String cancelUrl
    );

    /**
     * Query payment status from PayOS API.
     */
    PayOSPaymentInfoResponse getPaymentInfo(Long orderCode);
}
