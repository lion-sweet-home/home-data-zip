package org.example.homedatazip.subscription.dto;

// 카드 등록 → billingKey 발급 (요청)
// POST /api/payments/billing-keys

public record BillingKeyIssueRequest(
        String authKey
) {}