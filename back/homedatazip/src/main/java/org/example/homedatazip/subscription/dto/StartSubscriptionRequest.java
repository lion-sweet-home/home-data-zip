package org.example.homedatazip.subscription.dto;

public record StartSubscriptionRequest(
        String name,
        Long price,
        int periodDays
) {}