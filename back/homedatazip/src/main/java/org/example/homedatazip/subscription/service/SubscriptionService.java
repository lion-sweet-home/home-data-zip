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
import org.example.homedatazip.role.RoleType;
import org.example.homedatazip.role.repository.RoleRepository;
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

    @Transactional
    public void successBillingAuthByCustomerKey(BillingAuthSuccessRequest req) {
        Long userId = parseUserIdFromCustomerKey(req.customerKey());
        registerBillingKey(userId, req.customerKey(), req.authKey());
    }

    @Transactional
    public void registerBillingKey(Long userId, String customerKey, String authKey) {
        if (customerKey == null || customerKey.isBlank()) {
            throw new BusinessException(PaymentErrorCode.INVALID_CUSTOMER_KEY);
        }
        if (authKey == null || authKey.isBlank()) {
            throw new BusinessException(SubscriptionErrorCode.BILLING_AUTH_KEY_REQUIRED);
        }

        User user = getUser(userId);

        log.info("userId: {}", user.getId());

        Subscription sub = subscriptionRepository.findBySubscriber_Id(user.getId())
                .orElseGet(() -> subscriptionRepository.save(Subscription.createInitial(user)));

        log.info("sub: {}", sub.getId());

        var res = tossPaymentClient.issueBillingKey(authKey, user.getCustomerKey());

        sub.registerBillingKey(res.billingKey());
    }

    public BillingKeyIssueResponse issueBillingKey(Long userId, BillingKeyIssueRequest req) {
        User user = getUser(userId);

        // 요청이 null로 올 수 있으니 기본값 처리
        String orderName = (req != null && req.orderName() != null && !req.orderName().isBlank())
                ? req.orderName()
                : PLAN_NAME;

        Long amount = (req != null && req.amount() != null && req.amount() > 0)
                ? req.amount()
                : PRICE; //  기본 9900

        log.info("[BILLING] issueBillingKey userId={}, customerKey={}, orderName={}, amount={}, successUrl={}, failUrl={}",
                userId, user.getCustomerKey(), orderName, amount, successUrl, failUrl);

        return new BillingKeyIssueResponse(
                user.getCustomerKey(), // "CUSTOMER_1" 이런 거
                orderName,
                amount,                //  0이 아니라 9900 or req.amount
                successUrl,
                failUrl
        );
    }

    @Transactional
    public void revokeBillingKey(Long userId) {
        Subscription sub = getSubscription(userId);

        if (!sub.hasBillingKey()) {
            return; // 멱등
        }


        sub.clearBillingKey();
    }

    @Transactional
    public void successBillingAuth(Long userId, BillingAuthSuccessRequest request) {
        User user = getUser(userId);

        String authKey = request.authKey();
        if (authKey == null || authKey.isBlank()) {
            throw new BusinessException(SubscriptionErrorCode.BILLING_AUTH_KEY_REQUIRED);
        }

        var res = tossPaymentClient.issueBillingKey(authKey, user.getCustomerKey());

        Subscription sub = subscriptionRepository.findBySubscriber_Id(userId)
                .orElseGet(() -> subscriptionRepository.save(Subscription.createInitial(user)));

        sub.registerBillingKey(res.billingKey());
    }

    @Transactional
    public void startSubscription(Long userId) {
        Subscription sub = getSubscription(userId);

        // 카드 등록(billingKey) 체크 (기존)
        if (!sub.hasBillingKey()) {
            throw new BusinessException(SubscriptionErrorCode.BILLING_KEY_NOT_REGISTERED);
        }

        // 전화번호 인증 체크 (추가)
        User user = getUser(userId);
        if (!user.isPhoneVerified()) {
            throw new BusinessException(SubscriptionErrorCode.PHONE_NOT_VERIFIED);
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

        sub.start(today, PRICE);          // status ACTIVE + 기간 세팅
        grantSeller(sub.getSubscriber()); // SELLER 부여
    }


    @Transactional
    public void cancelAutoPay(Long userId) {
        getSubscription(userId).cancelAutoPay(); // status CANCELED, 권한(endDate까지) 유지
    }

    @Transactional
    public void reactivateAutoPay(Long userId) {
        Subscription sub = getSubscription(userId);

        if (!sub.hasBillingKey()) {
            throw new BusinessException(SubscriptionErrorCode.BILLING_KEY_NOT_REGISTERED);
        }

        if (sub.getStatus() == SubscriptionStatus.EXPIRED) {
            // 결제 없이 ACTIVE 복구 금지
            throw new BusinessException(SubscriptionErrorCode.CANNOT_REACTIVATE_EXPIRED);
        }

        // 여기서 가능한 케이스는 사실상 CANCELED
        sub.activateAutoPay();            // status ACTIVE
        grantSeller(sub.getSubscriber()); // SELLER 부여
    }

    @Transactional(readOnly = true)
    public SubscriptionMeResponse getMySubscription(Long userId) {
        return subscriptionRepository.findBySubscriber_Id(userId)
                .map(SubscriptionMeResponse::from)
                .orElseGet(SubscriptionMeResponse::none);
    }

    // ===== private =====

    private Subscription getSubscription(Long userId) {
        return subscriptionRepository.findBySubscriber_Id(userId)
                .orElseThrow(() -> new BusinessException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));
    }

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
