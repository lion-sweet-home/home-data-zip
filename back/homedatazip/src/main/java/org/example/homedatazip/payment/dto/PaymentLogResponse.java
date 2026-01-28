package org.example.homedatazip.payment.dto;

import org.example.homedatazip.payment.entity.PaymentLog;
import org.example.homedatazip.payment.type.PaymentStatus;

import java.time.LocalDateTime;

//결제 로그 1건 응답
public record PaymentLogResponse(
        Long paymentLogId,
        Long subscriptionId,
        String orderId,
        String paymentKey,
        String orderName,
        Long amount,
        PaymentStatus paymentStatus,
        String failReason,
        LocalDateTime approvedAt,
        LocalDateTime paidAt
) {
    public static PaymentLogResponse from(PaymentLog p) {
        return new PaymentLogResponse(
                p.getId(),
                p.getSubscription().getId(),
                p.getOrderId(),
                p.getPaymentKey(),
                p.getOrderName(),
                p.getAmount(),
                p.getPaymentStatus(),
                p.getFailReason(),
                p.getApprovedAt(),
                p.getPaidAt()
        );
    }
}