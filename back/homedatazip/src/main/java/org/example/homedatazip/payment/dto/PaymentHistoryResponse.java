package org.example.homedatazip.payment.dto;

import java.time.LocalDateTime;

public record PaymentHistoryResponse(

        String orderId,
        String orderName,
        Long amount,
        String paymentStatus,
        LocalDateTime paidAt
) {}