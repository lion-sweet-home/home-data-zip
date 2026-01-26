package org.example.homedatazip.subscription.dto;

import org.example.homedatazip.subscription.entity.Subscription;
import org.example.homedatazip.subscription.type.SubscriptionStatus;

import java.time.LocalDate;

// 구독 해지 (자동결제만 중단)
// POST /api/subscriptions/{subscriptionId}/cancel

public record SubscriptionCancelResponse(
        Long subscriptionId,
        SubscriptionStatus status,
        boolean isActive,
        LocalDate endDate
) {
    public static SubscriptionCancelResponse from(Subscription s) {
        return new SubscriptionCancelResponse(
                s.getId(),
                s.getStatus(),
                s.isActive(),
                s.getEndDate()
        );
    }
}