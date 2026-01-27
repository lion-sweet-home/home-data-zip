package org.example.homedatazip.payment.dto;

import java.time.LocalDateTime;

//토스 승인 성공 응답
public record PaymentConfirmResponse(
        String paymentKey,
        String orderId,
        String orderName,
        Long amount,
        LocalDateTime approvedAt
) {}