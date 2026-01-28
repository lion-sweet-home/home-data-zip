package org.example.homedatazip.payment.dto;

public record PaymentConfirmRequest(
        String paymentKey,
        String orderId,
        Long amount
) {}