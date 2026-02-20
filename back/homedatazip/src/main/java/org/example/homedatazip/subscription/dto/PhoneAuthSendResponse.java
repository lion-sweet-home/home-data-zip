package org.example.homedatazip.subscription.dto;

public record PhoneAuthSendResponse(
        String requestId,
        int expiresInSeconds
) {}
