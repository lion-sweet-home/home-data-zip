package org.example.homedatazip.payment.dto;

//정기결제/빌링 결제 승인 요청
public record BillingConfirmRequest(
        Long subscriptionId,
        String orderId,
        String orderName,
        Long amount
) {}