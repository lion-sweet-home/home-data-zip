package org.example.homedatazip.payment.client.dto;

public record TossBillingKeyIssueRequest(
        String authKey,
        String customerKey
) {}
