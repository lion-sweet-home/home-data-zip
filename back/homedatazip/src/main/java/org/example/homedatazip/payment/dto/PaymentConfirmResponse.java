package org.example.homedatazip.payment.dto;

import org.example.homedatazip.subscription.type.SubscriptionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PaymentConfirmResponse(
        String paymentKey,
        String orderId,
        Long amount,
        LocalDateTime approvedAt,

        Long subscriptionId,
        SubscriptionStatus subscriptionStatus,
        LocalDate startDate,
        LocalDate endDate
) {}