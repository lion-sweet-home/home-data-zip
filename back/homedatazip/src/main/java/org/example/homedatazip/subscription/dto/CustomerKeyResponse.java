package org.example.homedatazip.subscription.dto;

// customerKey 조회
// GET /api/payments/customer-key DTO

public record CustomerKeyResponse(
        String customerKey
) {
}