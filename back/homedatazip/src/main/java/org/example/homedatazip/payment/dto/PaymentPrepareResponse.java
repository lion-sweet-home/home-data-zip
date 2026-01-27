package org.example.homedatazip.payment.dto;

public record PaymentPrepareResponse(
        String customerKey,
        String orderId,
        String orderName,
        Long amount
) {}