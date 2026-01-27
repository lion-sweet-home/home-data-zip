package org.example.homedatazip.payment.dto;

import java.time.LocalDateTime;

/**
 * ✅ 토스 단건결제 승인 결과(토스에서 내려주는 값 중심)
 * - paymentKey: 토스 결제건 식별자(승인/취소/조회에 필요)
 * - orderId: 우리 주문번호
 * - amount: 결제금액
 * - approvedAt: 토스 승인시간
 */
public record TossPaymentConfirmResponse(
        String paymentKey,
        String orderId,
        Long amount,
        LocalDateTime approvedAt
) {}
