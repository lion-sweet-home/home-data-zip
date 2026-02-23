package org.example.homedatazip.subscription.dto;

import org.example.homedatazip.subscription.entity.Subscription;
import org.example.homedatazip.subscription.type.SubscriptionStatus;

import java.time.LocalDate;

public record SubscriptionMeResponse(
        Long subscriptionId,
        String planName,
        Long price,
        SubscriptionStatus status,
        boolean isActive,
        LocalDate startDate,
        LocalDate endDate,
        boolean hasBillingKey
) {
    public static SubscriptionMeResponse from(Subscription s) {
        return new SubscriptionMeResponse(
                s.getId(),
                s.getName(),
                s.getPrice(),
                s.getStatus(),
                s.isActive(),
                s.getStartDate(),
                s.getEndDate(),
                s.getBillingKey() != null && !s.getBillingKey().isBlank()
        );
    }

    public static SubscriptionMeResponse none() {
        return new SubscriptionMeResponse(
                null,
                null,
                0L,
                SubscriptionStatus.NONE,
                false,
                null,
                null,
                false
        );
    }
}