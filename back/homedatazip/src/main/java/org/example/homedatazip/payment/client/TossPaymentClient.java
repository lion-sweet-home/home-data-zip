package org.example.homedatazip.payment.client;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.exception.BusinessException;
import org.example.homedatazip.global.exception.domain.PaymentErrorCode;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class TossPaymentClient {

    /**
     * 결제 승인(단건 결제)
     * - 실제 토스 승인 API 호출하는 자리
     */
    public TossApproveResult approve(String paymentKey, String orderId, Long amount) {
        try {
            // TODO: 실제 토스 승인 API 호출 구현
            // 지금은 샘플 성공 응답
            return new TossApproveResult(paymentKey, orderId, amount, LocalDateTime.now());
        } catch (Exception e) {
            throw new BusinessException(PaymentErrorCode.TOSS_APPROVE_FAILED);
        }
    }

    /**
     * 정기결제(빌링키 결제)
     * - 실제 토스 빌링 결제 API 호출하는 자리
     */
    public TossBillingResult chargeBilling(String customerKey, String billingKey, String orderId, String orderName, Long amount) {
        try {
            // TODO: 실제 토스 빌링 결제 API 호출 구현
            return new TossBillingResult(orderId, amount, LocalDateTime.now());
        } catch (Exception e) {
            throw new BusinessException(PaymentErrorCode.TOSS_BILLING_FAILED);
        }
    }

    // ----- client 결과 DTO -----

    public record TossApproveResult(
            String paymentKey,
            String orderId,
            Long amount,
            LocalDateTime approvedAt
    ) {}

    public record TossBillingResult(
            String orderId,
            Long amount,
            LocalDateTime approvedAt
    ) {}
}
