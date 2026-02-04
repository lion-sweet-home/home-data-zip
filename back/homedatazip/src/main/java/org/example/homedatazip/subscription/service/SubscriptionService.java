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
import org.example.homedatazip.role.Role;
import org.example.homedatazip.role.repository.RoleRepository;
import org.example.homedatazip.role.RoleType;
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

    private final RoleRepository roleRepository;

    @Value("${payment.toss.billing-success-url}")
    private String successUrl;

    @Value("${payment.toss.billing-fail-url}")
    private String failUrl;

    /**
     * 토스 successUrl 콜백용 (JWT 없음)
     */
    @Transactional
    public void successBillingAuthByCustomerKey(BillingAuthSuccessRequest req) {
        String customerKey = req.customerKey();
        String authKey = req.authKey();

        Long userId = parseUserIdFromCustomerKey(customerKey);

        registerBillingKey(userId, customerKey, authKey);
    }


     // 테스트 용 : billingKey 등록 처리

    @Transactional
    public void registerBillingKey(Long userId, String customerKey, String authKey) {
        if (customerKey == null || customerKey.isBlank()) {
            throw new BusinessException(PaymentErrorCode.INVALID_CUSTOMER_KEY);
        }
        if (authKey == null || authKey.isBlank()) {
            throw new BusinessException(SubscriptionErrorCode.BILLING_AUTH_KEY_REQUIRED);
        }

        User user = getUser(userId);

        Subscription sub = subscriptionRepository.findBySubscriber_Id(user.getId())
                .orElseGet(() ->
                        subscriptionRepository.save(
                                Subscription.createInitial(user)
                        )
                );

        String billingKey = authKey.startsWith("bln_")
                ? authKey
                : tossPaymentClient.issueBillingKey(authKey, user.getCustomerKey()).billingKey();

        sub.registerBillingKey(billingKey);
    }


     // 카드 등록 시작

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
     * 로그인 상태 API용
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

    /**
     * 첫 결제
     * ACTIVE 되는 순간 SELLER 부여
     */
    @Transactional
    public void startSubscription(Long userId) {
        Subscription sub = getSubscription(userId);

        if (!sub.hasBillingKey()) {
            throw new BusinessException(SubscriptionErrorCode.BILLING_KEY_NOT_REGISTERED);
        }

        LocalDate today = LocalDate.now();

        // 이미 ACTIVE면 멱등
        if (sub.getStatus() == SubscriptionStatus.ACTIVE) {
            return;
        }

        // CANCELED인데 endDate 남아있으면 즉시 결제 없이 ACTIVE만 복구
        if (sub.getStatus() == SubscriptionStatus.CANCELED) {
            LocalDate endDate = sub.getEndDate();
            if (endDate != null && !endDate.isBefore(today)) {
                sub.activateAutoPay(); // status ACTIVE
                grantSeller(sub.getSubscriber());
                return;
            }
        }

        // 즉시 결제 케이스
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

        // 결제 성공 → 구독 시작(기간 갱신, status ACTIVE)
        sub.start(LocalDate.now(), PRICE);


        grantSeller(sub.getSubscriber());
    }

    // 자동결제 OFF
    @Transactional
    public void cancelAutoPay(Long userId) {
        getSubscription(userId).cancelAutoPay();
    }


    // 자동결제 ON
    @Transactional
    public void reactivateAutoPay(Long userId) {
        Subscription sub = getSubscription(userId);

        if (!sub.hasBillingKey()) {
            throw new BusinessException(SubscriptionErrorCode.BILLING_KEY_NOT_REGISTERED);
        }

        sub.activateAutoPay();           // status ACTIVE
        grantSeller(sub.getSubscriber()); //  SELLER 부여
    }

    // 내 구독 조회
    public SubscriptionMeResponse getMySubscription(Long userId) {
        return SubscriptionMeResponse.from(getSubscription(userId));
    }

    private Subscription getSubscription(Long userId) {
        return subscriptionRepository.findBySubscriber_Id(userId)
                .orElseThrow(() -> new BusinessException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));
    }

    // ===== private =====

    private void grantSeller(User user) {
        if (user.hasRole(RoleType.SELLER)) return;

        Role seller = roleRepository.findByRoleType(RoleType.SELLER)
                .orElseThrow(() -> new BusinessException(SubscriptionErrorCode.ROLE_NOT_FOUND));

        user.addRole(seller);
    }

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
}
