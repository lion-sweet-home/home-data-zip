package org.example.homedatazip.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TossPaymentConfirmResponse(
        String paymentKey,
        String orderId,
        Long totalAmount,
        String approvedAt
) {}
