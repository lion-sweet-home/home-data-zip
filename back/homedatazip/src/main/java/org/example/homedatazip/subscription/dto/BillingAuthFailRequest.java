package org.example.homedatazip.subscription.dto;

public record BillingAuthFailRequest(
        String customerKey,
        String code,
        String message
) {}