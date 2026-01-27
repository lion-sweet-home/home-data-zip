package org.example.homedatazip.payment.dto;

import java.time.LocalDateTime;

public record PaymentApproveResponse(

        Long paymentLogId,
        String orderId,
        Long amount,
        String paymentStatus,
        LocalDateTime approvedAt
) {}