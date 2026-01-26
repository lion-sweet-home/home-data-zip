package org.example.homedatazip.subscription.dto;

// 카드 등록 → billingKey 발급 (응답)
// POST /api/payments/billing-keys

public record BillingKeyIssueResponse(
        boolean hasBillingKey
) {}