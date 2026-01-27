package org.example.homedatazip.subscription.dto;

public record StartSubscriptionRequest(
        Long subscriberId,
        String name,
        Long price,
        int periodDays
) {}