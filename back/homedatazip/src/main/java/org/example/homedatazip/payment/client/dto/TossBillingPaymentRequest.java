package org.example.homedatazip.payment.client.dto;

/**
 * Toss Billing 결제 요청
 * POST /v1/billing/{billingKey}
 */
public record TossBillingPaymentRequest(
        String customerKey,
        Long amount,
        String orderId,
        String orderName
) {}
