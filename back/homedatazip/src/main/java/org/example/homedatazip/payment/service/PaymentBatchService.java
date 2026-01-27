package org.example.homedatazip.payment.service;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.payment.client.TossPaymentClient;
import org.example.homedatazip.subscription.entity.Subscription;
import org.example.homedatazip.subscription.repository.SubscriptionRepository;
import org.example.homedatazip.subscription.type.SubscriptionStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentBatchService {

    private final SubscriptionRepository subscriptionRepository;
    private final TossPaymentClient tossPaymentClient;
    private final PaymentService paymentService;

    /**
     * 매일 00:10에 정기결제 수행(예시)
     * - endDate == today 인 ACTIVE 구독 대상
     * - 성공: PaymentLog 저장 + 구독 +1개월
     * - 실패: PaymentLog 저장 + 구독 CANCELED
     */
    @Transactional
    @Scheduled(cron = "0 10 0 * * *")
    public void runRecurringPayments() {

        LocalDate today = LocalDate.now();

        List<Subscription> targets = subscriptionRepository.findAllByIsActiveTrueAndStatusAndEndDateEqual(
                SubscriptionStatus.ACTIVE,
                today
        );

        for (Subscription sub : targets) {

            String customerKey = sub.getSubscriber().getCustomerKey(); // 너가 User에 만든 메서드
            String billingKey = sub.getBillingKey();                   // Subscription에 있음
            String orderId = "BILL-" + UUID.randomUUID();
            String orderName = "정기 구독 결제";
            Long amount = sub.getPrice() == null ? 9900L : sub.getPrice();

            try {
                var result = tossPaymentClient.chargeBilling(customerKey, billingKey, orderId, orderName, amount);
                paymentService.recordRecurringSuccess(sub, orderId, orderName, amount, result.approvedAt());
            } catch (Exception e) {
                // Client에서 BusinessException 던지면 그대로 잡혀도 되고,
                // 이유 메시지로 남기고 싶으면 e.getMessage() 활용
                paymentService.recordRecurringFailure(sub, orderId, orderName, amount, e.getMessage());
            }
        }
    }
}
