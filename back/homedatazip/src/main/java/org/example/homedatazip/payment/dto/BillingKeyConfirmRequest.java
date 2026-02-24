package org.example.homedatazip.payment.dto;

public record BillingKeyConfirmRequest(
        String authKey,
        String customerKey
) {}