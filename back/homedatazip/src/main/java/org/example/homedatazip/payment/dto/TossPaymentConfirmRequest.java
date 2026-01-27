package org.example.homedatazip.payment.dto;

//결제 승인 요청
public record TossPaymentConfirmRequest(
        String paymentKey,
        String orderId,
        Long amount
) {}