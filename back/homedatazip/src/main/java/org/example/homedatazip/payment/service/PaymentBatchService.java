package org.example.homedatazip.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.payment.client.TossPaymentClient;
import org.example.homedatazip.payment.client.dto.TossBillingPaymentResponse;
import org.example.homedatazip.payment.dto.BillingRecurringResultResponse;
import org.example.homedatazip.payment.entity.PaymentLog;
import org.example.homedatazip.payment.repository.PaymentLogRepository;
import org.example.homedatazip.subscription.entity.Subscription;
import org.example.homedatazip.subscription.repository.SubscriptionRepository;
import org.example.homedatazip.subscription.type.SubscriptionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentBatchService {

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentLogRepository paymentLogRepository;
    private final TossPaymentClient tossPaymentClient;

    public void run(LocalDate today) {
        runRecurringPayment(today);
    }

    /**
     * 정기결제 실행
     * - 정책: "만료일(endDate) + 1일"에 결제
     */
    public BillingRecurringResultResponse runRecurringPayment(LocalDate today) {

        if (today == null) {
            today = LocalDate.now();
        }

        LocalDate targetEndDate = today.minusDays(1);

        List<Subscription> targets =
                subscriptionRepository.findAllByIsActiveTrueAndStatusAndEndDate(
                        SubscriptionStatus.ACTIVE,
                        targetEndDate
                );

        int success = 0;
        int fail = 0;

        for (Subscription sub : targets) {
            if (!sub.hasBillingKey()) {
                // billingKey 없으면 결제 불가 -> 만료
                sub.expire();
                fail++;
                continue;
            }

            // 오늘 결제 실행을 1회로 고정(멱등)
            String orderId = "SUB_RENEW_" + sub.getId() + "_" + today;

            if (paymentLogRepository.existsByOrderId(orderId)) {
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
                TossBillingPaymentResponse res = tossPaymentClient.payWithBillingKey(
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
                        LocalDateTime.now()
                );

                //  결제 성공 -> 1개월 연장
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
