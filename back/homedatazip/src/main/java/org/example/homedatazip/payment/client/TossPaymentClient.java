package org.example.homedatazip.payment.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.global.exception.BusinessException;
import org.example.homedatazip.global.exception.domain.PaymentErrorCode;
import org.example.homedatazip.payment.dto.TossConfirmResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class TossPaymentClient {

    /**
     * 단건 결제 승인 (지금은 샘플)
     */
    public TossConfirmResponse approve(String paymentKey, String orderId, Long amount) {
        try {
            // TODO: 실제 토스 승인 API 호출
            return new TossConfirmResponse(paymentKey, orderId, amount, LocalDateTime.now());
        } catch (Exception e) {
            log.error("[TossPaymentClient] approve failed. orderId={}, amount={}, msg={}",
                    orderId, amount, e.getMessage(), e);
            throw new BusinessException(PaymentErrorCode.TOSS_APPROVE_FAILED);
        }
    }

    /**
     * 정기결제(빌링키 결제) (지금은 샘플)
     */
    public TossConfirmResponse chargeBilling(
            String customerKey,
            String billingKey,
            String orderId,
            String orderName,
            Long amount
    ) {
        try {
            // TODO: 실제 토스 빌링 결제 API 호출
            // billing 결제의 응답 paymentKey가 있으면 여기서 세팅
            String paymentKey = null;

            return new TossConfirmResponse(paymentKey, orderId, amount, LocalDateTime.now());
        } catch (Exception e) {
            log.error("[TossPaymentClient] chargeBilling failed. orderId={}, amount={}, msg={}",
                    orderId, amount, e.getMessage(), e);
            throw new BusinessException(PaymentErrorCode.TOSS_BILLING_FAILED);
        }
    }
}
