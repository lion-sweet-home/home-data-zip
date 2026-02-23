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

        Subscription sub = subscriptionRepository.findBySubscriber_Id(user.getId())
                .orElseGet(() -> subscriptionRepository.save(Subscription.createInitial(user)));

        var res = tossPaymentClient.issueBillingKey(authKey, user.getCustomerKey());

        sub.registerBillingKey(res.billingKey());
        subscriptionRepository.flush();

        log.info("[BILLING] saved billingKey userId={}, subId={}, billingKeyIssuedAt={}",
                user.getId(), sub.getId(), sub.getBillingKeyIssuedAt());
    }

    @Transactional
    public void successBillingAuth(Long userId, BillingAuthSuccessRequest request) {
        registerBillingKey(userId, getUser(userId).getCustomerKey(), request.authKey());
    }

    @Transactional(readOnly = true)
    public BillingKeyIssueResponse issueBillingKey(Long userId, BillingKeyIssueRequest req) {
        User user = getUser(userId);

        String orderName = (req != null && req.orderName() != null && !req.orderName().isBlank())
                ? req.orderName()
                : PLAN_NAME;

        Long amount = (req != null && req.amount() != null && req.amount() > 0)
                ? req.amount()
                : PRICE;

        log.info("[BILLING] issueBillingKey userId={}, customerKey={}, orderName={}, amount={}, successUrl={}, failUrl={}",
                userId, user.getCustomerKey(), orderName, amount, successUrl, failUrl);

        return new BillingKeyIssueResponse(
                user.getCustomerKey(),
                orderName,
                amount,
                successUrl,
                failUrl
        );
    }

    @Transactional
    public void revokeBillingKey(Long userId) {
        Subscription sub = getSubscription(userId);

        if (!sub.hasBillingKey()) return;

        sub.clearBillingKey();
        subscriptionRepository.flush();
    }

    /**
     * start 시도 순간 subscription row 생성 보장
     * 그리고 결제 성공 후 SELLER 부여하는 과정에서
     * user.roles 안에 role=null 같은 쓰레기 UserRole이 섞여 있으면
     * flush 때 같이 insert 되면서 role_id null 터진다.
     * -> flush 전에 roles 정리하고, userRepository.save(user) 호출하지 말고, managed 상태로 dirty checking에 맡긴다.
     */
    @Transactional
    public void startSubscription(Long userId) {
        User user = getUser(userId);

        Subscription sub = subscriptionRepository.findBySubscriber_Id(userId)
                .orElseGet(() -> subscriptionRepository.save(Subscription.createInitial(user)));

        // row 생성/조회 결과 확정
        subscriptionRepository.flush();

        log.info("[SUB_START] start called userId={}, subId={}, status={}, hasBillingKey={}, phoneVerified={}",
                userId, sub.getId(), sub.getStatus(), sub.hasBillingKey(), user.isPhoneVerified());

        if (!user.isPhoneVerified()) {
            throw new BusinessException(SubscriptionErrorCode.PHONE_NOT_VERIFIED);
        }

        if (!sub.hasBillingKey()) {
            throw new BusinessException(SubscriptionErrorCode.BILLING_KEY_NOT_REGISTERED);
        }

        LocalDate today = LocalDate.now();

        if (sub.getStatus() == SubscriptionStatus.ACTIVE) {
            return;
        }

        if (sub.getStatus() == SubscriptionStatus.CANCELED) {
            LocalDate endDate = sub.getEndDate();
            if (endDate != null && !endDate.isBefore(today)) {
                sub.activateAutoPay();

                // ✅ flush 전에 roles 쓰레기 정리
                normalizeUserRoles(user);

                grantSeller(user);

                // ✅ userRepository.save(user) 하지 마
                // managed entity라 dirty checking으로 충분함
                subscriptionRepository.flush();
                return;
            }
        }

        String orderId = "SUB_START_" + UUID.randomUUID();

        PaymentLog logEntity = paymentLogRepository.save(
                PaymentLog.createProcessing(sub, orderId, PLAN_NAME, PRICE)
        );

        TossBillingPaymentResponse res = tossPaymentClient.payWithBillingKey(
                sub.getBillingKey(),
                user.getCustomerKey(),
                orderId,
                PLAN_NAME,
                PRICE
        );

        if (!PRICE.equals(res.totalAmount())) {
            throw new BusinessException(PaymentErrorCode.INVALID_AMOUNT);
        }

        logEntity.markApproved(
                res.paymentKey(),
                res.orderId(),
                PRICE,
                LocalDateTime.now()
        );

        sub.start(today, PRICE);

        // ✅ flush 전에 roles 쓰레기 정리
        normalizeUserRoles(user);

        grantSeller(user);

        // ✅ userRepository.save(user) 하지 마
        subscriptionRepository.flush();
    }

    @Transactional
    public void cancelAutoPay(Long userId) {
        getSubscription(userId).cancelAutoPay();
        subscriptionRepository.flush();
    }

    @Transactional
    public void reactivateAutoPay(Long userId) {
        Subscription sub = getSubscription(userId);

        if (!sub.hasBillingKey()) {
            throw new BusinessException(SubscriptionErrorCode.BILLING_KEY_NOT_REGISTERED);
        }

        if (sub.getStatus() == SubscriptionStatus.EXPIRED) {
            throw new BusinessException(SubscriptionErrorCode.CANNOT_REACTIVATE_EXPIRED);
        }

        sub.activateAutoPay();

        User user = getUser(userId);

        normalizeUserRoles(user);
        grantSeller(user);

        subscriptionRepository.flush();
    }

    @Transactional(readOnly = true)
    public SubscriptionMeResponse getMySubscription(Long userId) {
        return subscriptionRepository.findBySubscriber_Id(userId)
                .map(SubscriptionMeResponse::from)
                .orElseGet(SubscriptionMeResponse::none);
    }

    // ===== private =====

    /**
     * ✅ 핵심: roles 컬렉션에 role=null / role.id=null / user=null 같은 쓰레기 UserRole 있으면
     * flush 때 같이 persist 되면서 role_id null로 터진다.
     * 그래서 flush 직전에 싹 청소.
     */
    private void normalizeUserRoles(User user) {
        if (user.getRoles() == null) return;

        int before = user.getRoles().size();

        user.getRoles().removeIf(ur ->
                ur == null ||
                        ur.getUser() == null ||
                        ur.getRole() == null ||
                        ur.getRole().getId() == null
        );

        int after = user.getRoles().size();

        if (before != after) {
            log.warn("[normalizeUserRoles] cleaned invalid roles. userId={}, before={}, after={}",
                    user.getId(), before, after);
        }
    }

    private Subscription getSubscription(Long userId) {
        return subscriptionRepository.findBySubscriber_Id(userId)
                .orElseThrow(() -> new BusinessException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));
    }

    private void grantSeller(User user) {
        if (user.hasRole(RoleType.SELLER)) return;

        Role seller = roleRepository.findByRoleType(RoleType.SELLER)
                .orElseThrow(() -> new BusinessException(SubscriptionErrorCode.ROLE_NOT_FOUND));

        log.info("[grantSeller] seller.id={}, seller.type={}", seller.getId(), seller.getRoleType());

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