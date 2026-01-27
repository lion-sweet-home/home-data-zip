package org.example.homedatazip.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentApproveRequest(

        @NotBlank
        String orderId,
        @NotBlank
        String paymentKey, // 토스 결제 key
        @NotNull
        Long amount // 결제 금액 (검증용)
) {}