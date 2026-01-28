package org.example.homedatazip.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.payment.client.TossPaymentClient;
import org.example.homedatazip.payment.dto.BillingRecurringResultResponse;
import org.example.homedatazip.payment.entity.PaymentLog;
import org.example.homedatazip.payment.repository.PaymentLogRepository;
import org.example.homedatazip.subscription.entity.Subscription;
import org.example.homedatazip.subscription.repository.SubscriptionRepository;
import org.example.homedatazip.subscription.type.SubscriptionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentBatchService {

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentLogRepository paymentLogRepository;
    private final TossPaymentClient tossPaymentClient;

    /**
     * 기존 코드 호환용 (void)
     */
    public void run(LocalDate today) {
        runRecurringPayment(today);
    }

    /**
     * 스케줄러/관리자에서 호출하는 정기결제 실행
     * - endDate == today 인 ACTIVE 구독만 결제 시도
     * - 멱등: orderId 기준으로 중복 결제 방지
     */
    public BillingRecurringResultResponse runRecurringPayment(LocalDate today) {

        List<Subscription> targets =
                subscriptionRepository.findAllByIsActiveTrueAndStatusAndEndDate(
                        SubscriptionStatus.ACTIVE,
                        today
                );

        int success = 0;
        int fail = 0;

        for (Subscription sub : targets) {
            if (!sub.hasBillingKey()) {
                sub.expire();
                fail++;
                continue;
            }

            String orderId = "SUB_RENEW_" + sub.getId() + "_" + today;

            if (paymentLogRepository.existsByOrderId(orderId)) {
                // 이미 처리된 건 성공으로 간주(멱등)
                success++;
                continue;
            }

            PaymentLog paymentLog = paymentLogRepository.save(
                    PaymentLog.createProcessing(
                            sub,
                            orderId,
                            sub.getName(),
                            sub.getPrice()
                    )
            );

            try {
                var res = tossPaymentClient.payWithBillingKey(
                        sub.getBillingKey(),
                        sub.getSubscriber().getCustomerKey(),
                        orderId,
                        sub.getName(),
                        sub.getPrice()
                );

                paymentLog.markApproved(
                        res.paymentKey(),
                        res.orderId(),
                        sub.getPrice(),
                        today.atStartOfDay()
                );

                sub.extendOneMonth();
                success++;

            } catch (Exception e) {
                log.error("[BILLING] recurring payment failed. subscriptionId={}, orderId={}",
                        sub.getId(), orderId, e);

                paymentLog.markFailed("정기결제 실패");
                sub.expire();
                fail++;
            }
        }

        return new BillingRecurringResultResponse(targets.size(), success, fail);
    }
}
