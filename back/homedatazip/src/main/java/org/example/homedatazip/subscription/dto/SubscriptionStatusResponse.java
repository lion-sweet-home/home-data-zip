package org.example.homedatazip.subscription.dto;

import org.example.homedatazip.subscription.entity.Subscription;
import org.example.homedatazip.subscription.type.SubscriptionStatus;

import java.time.LocalDate;

public record SubscriptionStatusResponse(
        SubscriptionStatus status,
        boolean isActive,
        LocalDate endDate,
        boolean hasAccess
) {
    public static SubscriptionStatusResponse of(Subscription s, boolean hasAccess) {
        return new SubscriptionStatusResponse(
                s.getStatus(),
                s.isActive(),
                s.getEndDate(),
                hasAccess
        );
    }
}
