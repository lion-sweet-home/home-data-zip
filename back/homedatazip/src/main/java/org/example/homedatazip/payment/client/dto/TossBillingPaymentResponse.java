package org.example.homedatazip.payment.client.dto;

import java.time.OffsetDateTime;

/**
 * Toss Billing 결제 응답
 * POST /v1/billing/{billingKey}
 */
public record TossBillingPaymentResponse(

        String paymentKey,      // 토스 결제 키
        String orderId,         // 주문 ID
        Long totalAmount,       // 실제 결제 금액
        String status,          // DONE
        OffsetDateTime approvedAt // 결제 승인 시각
) {
}
