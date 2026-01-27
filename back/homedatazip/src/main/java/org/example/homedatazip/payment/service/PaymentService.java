package org.example.homedatazip.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.payment.client.TossPaymentClient;
import org.example.homedatazip.payment.dto.BillingConfirmRequest;
import org.example.homedatazip.payment.dto.BillingRecurringResultResponse;
import org.example.homedatazip.payment.dto.TossConfirmResponse;
import org.example.homedatazip.payment.entity.PaymentLog;
import org.example.homedatazip.payment.repository.PaymentLogRepository;
import org.example.homedatazip.payment.type.PaymentStatus;
import org.example.homedatazip.subscription.entity.Subscription;
import org.example.homedatazip.subscription.repository.SubscriptionRepository;
import org.example.homedatazip.subscription.type.SubscriptionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentLogRepository paymentLogRepository;
    private final TossPaymentClient tossPaymentClient;

    /**
     * 정기결제 배치(오늘 endDate == today인 ACTIVE만 처리)
     * - 성공: 결제로그(APPROVED) + 구독 1개월 연장
     * - 실패: 결제로그(FAILED) + 구독 자동결제 OFF(CANCELED)
     */
    @Transactional
    public BillingRecurringResultResponse processRecurringPayments(LocalDate today) {

        // 정기결제 대상: ACTIVE + isActive=true + endDate == today
        List<Subscription> targets = subscriptionRepository.findAllByIsActiveTrueAndStatusAndEndDateEqual(
                SubscriptionStatus.ACTIVE,
                today
        );

        int targetCount = targets.size();
        int successCount = 0;
        int failCount = 0;

        for (Subscription s : targets) {
            boolean ok = processOneRecurringPayment(s);
            if (ok) successCount++;
            else failCount++;
        }

        return new BillingRecurringResultResponse(targetCount, successCount, failCount);
    }

    /**
     * 구독 1건 정기결제 처리(핵심)
     */
    @Transactional
    public boolean processOneRecurringPayment(Subscription subscription) {
        LocalDateTime now = LocalDateTime.now();

        // billingKey 없으면 결제 불가 -> 자동결제 중단
        if (subscription.getBillingKey() == null || subscription.getBillingKey().isBlank()) {
            createFailLog(subscription, generateOrderId(), subscription.getName(), subscription.getPrice(),
                    "billingKey가 없어 정기결제를 진행할 수 없습니다.");
            subscription.cancelAutoPay(); // CANCELED
            return false;
        }

        String orderId = generateOrderId();
        String orderName = subscription.getName();
        Long amount = subscription.getPrice();

        try {
            BillingConfirmRequest req = new BillingConfirmRequest(
                    subscription.getId(),
                    orderId,
                    orderName,
                    amount
            );

            // ✅ TossPaymentClient 메서드에 맞춤: chargeBilling()
            TossConfirmResponse tossRes = tossPaymentClient.chargeBilling(
                    subscription.getSubscriber().getCustomerKey(), // User.getCustomerKey()
                    subscription.getBillingKey(),
                    req.orderId(),
                    req.orderName(),
                    req.amount()
            );

            // 성공 로그 저장
            PaymentLog logEntity = PaymentLog.builder()
                    .subscription(subscription)
                    .orderId(orderId)
                    .paymentKey(tossRes.paymentKey()) // billing 결제는 null일 수도 있음 (괜찮음)
                    .orderName(orderName)
                    .amount(amount)
                    .paymentStatus(PaymentStatus.APPROVED)
                    .approvedAt(tossRes.approvedAt() != null ? tossRes.approvedAt() : now)
                    .paidAt(now)
                    .build();

            paymentLogRepository.save(logEntity);

            // 성공: 기간 연장
            subscription.extendOneMonth();

            return true;

        } catch (Exception e) {
            log.warn("[PaymentService] recurring payment failed. subscriptionId={}, reason={}",
                    subscription.getId(), e.getMessage());

            createFailLog(subscription, orderId, orderName, amount, safeMsg(e));

            // 실패: 다음 결제 OFF
            subscription.cancelAutoPay();

            return false;
        }
    }

    private void createFailLog(Subscription subscription, String orderId, String orderName, Long amount, String reason) {
        PaymentLog logEntity = PaymentLog.builder()
                .subscription(subscription)
                .orderId(orderId)
                .orderName(orderName)
                .amount(amount)
                .paymentStatus(PaymentStatus.FAILED)
                .failReason(reason)
                .paidAt(LocalDateTime.now())
                .build();

        paymentLogRepository.save(logEntity);
    }

    private String generateOrderId() {
        return "ORD_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    }

    private String safeMsg(Exception e) {
        String msg = e.getMessage();
        return (msg == null || msg.isBlank()) ? "결제 실패(원인 미상)" : msg;
    }
}
