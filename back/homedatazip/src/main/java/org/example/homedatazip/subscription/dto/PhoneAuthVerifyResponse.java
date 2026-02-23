package org.example.homedatazip.subscription.dto;

public record PhoneAuthVerifyResponse(
        boolean verified,
        String verificationToken
) {}
