package org.example.homedatazip.payment.dto;

public record PaymentPrepareRequest(
        Long amount,        // 기본 9900
        String orderName    // 기본 "기본 요금제"
) {}