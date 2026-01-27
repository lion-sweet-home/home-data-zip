package org.example.homedatazip.payment.dto;

import java.time.LocalDateTime;

public record TossConfirmResponse(
        String paymentKey,
        String orderId,
        Long amount,
        LocalDateTime approvedAt
) {}