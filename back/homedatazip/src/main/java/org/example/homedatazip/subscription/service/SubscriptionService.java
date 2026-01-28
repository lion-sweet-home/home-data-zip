package org.example.homedatazip.subscription.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.global.exception.BusinessException;
import org.example.homedatazip.global.exception.domain.PaymentErrorCode;
import org.example.homedatazip.global.exception.domain.SubscriptionErrorCode;
import org.example.homedatazip.payment.client.TossPaymentClient;
import org.example.homedatazip.payment.client.dto.TossBillingPaymentResponse;
import org.example.homedatazip.payment.entity.PaymentLog;
import org.example.homedatazip.payment.repository.PaymentLogRepository;
import org.example.homedatazip.payment.type.PaymentStatus;
import org.example.homedatazip.subscription.dto.*;
import org.example.homedatazip.subscription.entity.Subscription;
import org.example.homedatazip.subscription.repository.SubscriptionRepository;
import org.example.homedatazip.subscription.type.SubscriptionStatus;
import org.example.homedatazip.user.entity.User;
import org.example.homedatazip.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionService {

    private static final String PLAN_NAME = "기본 요금제";
    private static final Long PRICE = 9900L;

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final PaymentLogRepository paymentLogRepository;
    private final TossPaymentClient tossPaymentClient;

    @Value("${payment.toss.billing-success-url}")
    private String successUrl;

    @Value("${payment.toss.billing-fail-url}")
    private String failUrl;

    /**
     * 카드 등록 시작
     */
    public BillingKeyIssueResponse issueBillingKey(Long userId, BillingKeyIssueRequest req) {
        User user = getUser(userId);

        return new BillingKeyIssueResponse(
                user.getCustomerKey(),
                PLAN_NAME,
                0L,
                successUrl,
                failUrl
        );
    }

    /**
     * authKey → billingKey 저장
     */
    @Transactional
    public void successBillingAuth(Long userId, BillingAuthSuccessRequest request) {
        User user = getUser(userId);

        var res = tossPaymentClient.issueBillingKey(
                request.authKey(),
                user.getCustomerKey()
        );

        Subscription sub = subscriptionRepository
                .findBySubscriber_Id(userId)
                .orElseGet(() ->
                        subscriptionRepository.save(
                                Subscription.createInitial(user)
                        )
                );

        sub.registerBillingKey(res.billingKey());
    }

    /**
     * 첫 결제 = 구독 시작 (billingKey 결제)
     */
    @Transactional
    public void startSubscription(Long userId) {
        Subscription sub = getSubscription(userId);

        if (!sub.hasBillingKey()) {
            throw new BusinessException(SubscriptionErrorCode.BILLING_KEY_NOT_REGISTERED);
        }

        if (sub.getStatus() == SubscriptionStatus.ACTIVE) {
            return;
        }

        String orderId = "SUB_START_" + UUID.randomUUID();

        // 결제 로그 선생성
        PaymentLog log = paymentLogRepository.save(
                PaymentLog.createProcessing(sub, orderId, PLAN_NAME, PRICE)
        );

        TossBillingPaymentResponse res =
                tossPaymentClient.payWithBillingKey(
                        sub.getBillingKey(),
                        sub.getSubscriber().getCustomerKey(),
                        orderId,
                        PLAN_NAME,
                        PRICE
                );

        if (!PRICE.equals(res.totalAmount())) {
            throw new BusinessException(PaymentErrorCode.INVALID_AMOUNT);
        }

        log.markApproved(
                res.paymentKey(),
                res.orderId(),
                PRICE,
                LocalDateTime.now()
        );

        sub.start(LocalDate.now(), PRICE);
    }

    /**
     * 자동결제 OFF
     */
    @Transactional
    public void cancelAutoPay(Long userId) {
        getSubscription(userId).cancelAutoPay();
    }

    /**
     * 자동결제 ON
     */
    @Transactional
    public void reactivateAutoPay(Long userId) {
        getSubscription(userId).activateAutoPay();
    }

    public SubscriptionMeResponse getMySubscription(Long userId) {
        return SubscriptionMeResponse.from(getSubscription(userId));
    }

    // ===== private =====

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new BusinessException(SubscriptionErrorCode.SUBSCRIBER_NOT_FOUND));
    }

    private Subscription getSubscription(Long userId) {
        return subscriptionRepository.findBySubscriber_Id(userId)
                .orElseThrow(() ->
                        new BusinessException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));
    }
}
