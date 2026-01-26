package org.example.homedatazip.subscription.dto;

import org.example.homedatazip.subscription.entity.Subscription;
import org.example.homedatazip.subscription.type.SubscriptionStatus;

import java.time.LocalDate;

// 내 구독 조회
// GET /api/subscriptions/me

public record SubscriptionMeResponse(
        Long subscriptionId,
        SubscriptionStatus status,
        boolean isActive,
        String name,
        Long price,
        LocalDate startDate,
        LocalDate endDate,
        boolean hasBillingKey
) {
    public static SubscriptionMeResponse from(Subscription s) {
        boolean hasBillingKey = s.getBillingKey() != null && !s.getBillingKey().isBlank();
        return new SubscriptionMeResponse(
                s.getId(),
                s.getStatus(),
                s.isActive(),
                s.getName(),
                s.getPrice(),
                s.getStartDate(),
                s.getEndDate(),
                hasBillingKey
        );
    }
}