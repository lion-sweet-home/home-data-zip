package org.example.homedatazip.subscription.dto;

public record BillingAuthSuccessRequest(
        String customerKey,
        String authKey // 또는 billingKey를 받는 방식이면 billingKey
) {}