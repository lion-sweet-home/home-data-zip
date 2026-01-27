package org.example.homedatazip.payment.dto;

//정기결제 배치 결과
public record BillingRecurringResultResponse(
        int targetCount,
        int successCount,
        int failCount
) {}