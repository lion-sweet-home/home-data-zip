package org.example.homedatazip.subscription.dto;

// 구독 시작 (최초 결제)
// POST /api/subscriptions

import org.example.homedatazip.subscription.entity.Subscription;
import org.example.homedatazip.subscription.type.SubscriptionStatus;

import java.time.LocalDate;

public record SubscriptionStartResponse(
        Long subscriptionId,
        SubscriptionStatus status,
        boolean isActive,
        String name,
        Long price,
        LocalDate startDate,
        LocalDate endDate
) {
    public static SubscriptionStartResponse from(Subscription s) {
        return new SubscriptionStartResponse(
                s.getId(),
                s.getStatus(),
                s.isActive(),
                s.getName(),
                s.getPrice(),
                s.getStartDate(),
                s.getEndDate()
        );
    }
}