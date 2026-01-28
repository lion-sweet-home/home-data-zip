package org.example.homedatazip.subscription.dto;

/**
 * 카드 등록 시작 응답 (프론트에서 Toss Billing 인증 시작할 때 사용)
 */
public record BillingKeyIssueResponse(
        String customerKey,
        String orderName,
        Long amount,
        String successUrl,
        String failUrl
) {}
