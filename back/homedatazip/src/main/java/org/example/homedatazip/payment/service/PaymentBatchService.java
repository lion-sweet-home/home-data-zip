package org.example.homedatazip.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.global.exception.BusinessException;
import org.example.homedatazip.global.exception.domain.PaymentErrorCode;
import org.example.homedatazip.payment.dto.BillingRecurringResultResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentBatchService {

    private final PaymentService paymentService;

    /**
     * ✅ 정기결제 배치 실행 메서드
     * - SubscriptionScheduler 또는 Admin 수동 실행 API에서 호출
     */
    @Transactional
    public BillingRecurringResultResponse runRecurringPayment(LocalDate date) {
        if (date == null) {
            throw new BusinessException(PaymentErrorCode.BATCH_DATE_REQUIRED);
        }

        try {
            BillingRecurringResultResponse res = paymentService.processRecurringPayments(date);

            log.info("[PaymentBatchService] runRecurringPayment done. date={}, target={}, success={}, fail={}",
                    date, res.targetCount(), res.successCount(), res.failCount());

            return res;

        } catch (Exception e) {
            log.error("[PaymentBatchService] runRecurringPayment failed. date={}", date, e);
            throw new BusinessException(PaymentErrorCode.BATCH_RECURRING_FAILED);
        }
    }
}
