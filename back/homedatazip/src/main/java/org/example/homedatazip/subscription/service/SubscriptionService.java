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
     * 토스 successUrl 콜백용 (JWT 없음)
     * - customerKey=CUSTOMER_{userId}에서 userId 파싱
     * - authKey가 bln_ 이면 billingKey로 간주
     * - 아니면 issueBillingKey(authKey)로 billingKey 발급 받아 저장
     */
    @Transactional
    public void successBillingAuthByCustomerKey(BillingAuthSuccessRequest req) {
        String customerKey = req.customerKey();
        String authKey = req.authKey();

        Long userId = parseUserIdFromCustomerKey(customerKey);

        // 공통 로직으로 위임
        registerBillingKey(userId, customerKey, authKey);
    }

    /**
     * (콜백/테스트용) customerKey 규칙으로 userId를 알고 있을 때 billingKey 등록 처리
     * 컨트롤러에서 subscriptionService.registerBillingKey(userId, customerKey, authKey)로 호출 가능
     */
    @Transactional
    public void registerBillingKey(Long userId, String customerKey, String authKey) {
        if (customerKey == null || customerKey.isBlank()) {
            throw new BusinessException(PaymentErrorCode.INVALID_CUSTOMER_KEY);
        }
        if (authKey == null || authKey.isBlank()) {
            throw new BusinessException(SubscriptionErrorCode.BILLING_AUTH_KEY_REQUIRED);
        }

        User user = getUser(userId);

        // 구독 없으면 생성해서 저장
        Subscription sub = subscriptionRepository.findBySubscriber_Id(user.getId())
                .orElseGet(() ->
                        subscriptionRepository.save(
                                Subscription.createInitial(user)
                        )
                );

        // authKey가 bln_이면 이미 billingKey
        String billingKey = authKey.startsWith("bln_")
                ? authKey
                : tossPaymentClient.issueBillingKey(authKey, user.getCustomerKey()).billingKey();

        // billingKey 저장
        sub.registerBillingKey(billingKey);
    }

    /**
     * 카드 등록 시작 (프론트에 customerKey/successUrl/failUrl 내려줌)
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
     * 로그인 상태 API용 (JWT 있음)
     * authKey → billingKey 발급받아 저장
     */
    @Transactional
    public void successBillingAuth(Long userId, BillingAuthSuccessRequest request) {
        User user = getUser(userId);

        String authKey = request.authKey();
        if (authKey == null || authKey.isBlank()) {
            throw new BusinessException(SubscriptionErrorCode.BILLING_AUTH_KEY_REQUIRED);
        }

        var res = tossPaymentClient.issueBillingKey(authKey, user.getCustomerKey());

        Subscription sub = subscriptionRepository.findBySubscriber_Id(userId)
                .orElseGet(() ->
                        subscriptionRepository.save(
                                Subscription.createInitial(user)
                        )
                );

        sub.registerBillingKey(res.billingKey());
    }


    @Transactional
    public void startSubscription(Long userId) {
        Subscription sub = getSubscription(userId);

        if (!sub.hasBillingKey()) {
            throw new BusinessException(SubscriptionErrorCode.BILLING_KEY_NOT_REGISTERED);
        }

        LocalDate today = LocalDate.now();

        // 이미 자동결제 ON (ACTIVE)이면 멱등 처리
        if (sub.getStatus() == SubscriptionStatus.ACTIVE) {
            return;
        }

        // 취소 상태(CANCELED)인데 만료일이 남아있으면
        //    -> 즉시 결제 없이 autoPay만 다시 켠다 (배치가 endDate+1일에 결제)
        if (sub.getStatus() == SubscriptionStatus.CANCELED) {
            LocalDate endDate = sub.getEndDate(); // Subscription에 endDate getter가 있어야 함
            if (endDate != null && !endDate.isBefore(today)) {
                sub.activateAutoPay(); // status ACTIVE로 변경 (배치 대상 포함)
                return;
            }
            // 만료일이 이미 지났으면 아래 "즉시 결제" 로직으로 떨어짐
        }

        // 즉시 결제 케이스만
        String orderId = "SUB_START_" + UUID.randomUUID();

        PaymentLog log = paymentLogRepository.save(
                PaymentLog.createProcessing(sub, orderId, PLAN_NAME, PRICE)
        );

        TossBillingPaymentResponse res = tossPaymentClient.payWithBillingKey(
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

        // 결제 성공 → 구독 시작(기간 갱신)
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
     * 자동결제 ON (수동 리액티브 API)
     * - 프론트가 "구독하기"를 쓰면 사실상 startSubscription()이 알아서 처리함
     */
    @Transactional
    public void reactivateAutoPay(Long userId) {
        Subscription sub = getSubscription(userId);
        if (!sub.hasBillingKey()) {
            throw new BusinessException(SubscriptionErrorCode.BILLING_KEY_NOT_REGISTERED);
        }
        sub.activateAutoPay();
    }

    public SubscriptionMeResponse getMySubscription(Long userId) {
        return SubscriptionMeResponse.from(getSubscription(userId));
    }

    // ===== private =====

    private Long parseUserIdFromCustomerKey(String customerKey) {
        if (customerKey == null || customerKey.isBlank() || !customerKey.startsWith("CUSTOMER_")) {
            throw new BusinessException(PaymentErrorCode.INVALID_CUSTOMER_KEY);
        }
        try {
            return Long.parseLong(customerKey.substring("CUSTOMER_".length()));
        } catch (NumberFormatException e) {
            throw new BusinessException(PaymentErrorCode.INVALID_CUSTOMER_KEY);
        }
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(SubscriptionErrorCode.SUBSCRIBER_NOT_FOUND));
    }

    private Subscription getSubscription(Long userId) {
        return subscriptionRepository.findBySubscriber_Id(userId)
                .orElseThrow(() -> new BusinessException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));
    }
}
