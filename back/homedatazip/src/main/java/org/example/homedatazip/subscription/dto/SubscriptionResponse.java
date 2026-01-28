package org.example.homedatazip.subscription.dto;

import org.example.homedatazip.subscription.entity.Subscription;
import org.example.homedatazip.subscription.type.SubscriptionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SubscriptionResponse(
        Long subscriptionId,
        Long subscriberId,
        String planName,
        Long price,
        SubscriptionStatus status,
        boolean isActive,
        LocalDate startDate,
        LocalDate endDate,
        String billingKey,
        LocalDateTime billingKeyIssuedAt
) {
    public static SubscriptionResponse from(Subscription s) {
        return new SubscriptionResponse(
                s.getId(),
                s.getSubscriber().getId(),
                s.getName(),
                s.getPrice(),
                s.getStatus(),
                s.isActive(),
                s.getStartDate(),
                s.getEndDate(),
                s.getBillingKey(),
                s.getBillingKeyIssuedAt()
        );
    }
}
