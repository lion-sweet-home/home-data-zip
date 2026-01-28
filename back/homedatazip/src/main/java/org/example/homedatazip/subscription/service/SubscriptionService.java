package org.example.homedatazip.subscription.service;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.exception.BusinessException;
import org.example.homedatazip.global.exception.domain.SubscriptionErrorCode;
import org.example.homedatazip.payment.client.TossPaymentClient;
import org.example.homedatazip.payment.client.dto.TossBillingKeyIssueResponse;
import org.example.homedatazip.subscription.dto.SubscriptionMeResponse;
import org.example.homedatazip.subscription.entity.Subscription;
import org.example.homedatazip.subscription.repository.SubscriptionRepository;
import org.example.homedatazip.subscription.type.SubscriptionStatus;
import org.example.homedatazip.user.entity.User;
import org.example.homedatazip.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final TossPaymentClient tossPaymentClient;

    /**
     * ✅ 카드 등록 완료(authKey 수신) -> billingKey 발급/저장
     * - authKey는 billingKey가 아님
     * - Toss에 authKey를 보내서 billingKey로 "교환"해야 함
     */
    @Transactional
    public void registerBillingKey(Long subscriberId, String authKey) {

        if (authKey == null || authKey.isBlank()) {
            throw new BusinessException(SubscriptionErrorCode.BILLING_AUTH_KEY_REQUIRED);
        }

        User user = userRepository.findById(subscriberId)
                .orElseThrow(() -> new BusinessException(SubscriptionErrorCode.SUBSCRIBER_NOT_FOUND));

        // 구독 레코드 없으면 생성(카드등록은 '준비'라서 ACTIVE로 만들진 않음)
        Subscription s = subscriptionRepository.findBySubscriber_Id(subscriberId).orElse(null);
        if (s == null) {
            LocalDate today = LocalDate.now();
            s = Subscription.builder()
                    .subscriber(user)
                    .name("기본 요금제")
                    .price(9900L)
                    .status(SubscriptionStatus.EXPIRED)
                    .isActive(false)
                    .startDate(today)
                    .endDate(today)
                    .build();

            s = subscriptionRepository.save(s);
        }

        // customerKey는 User.getCustomerKey()만 사용
        TossBillingKeyIssueResponse res = tossPaymentClient.issueBillingKey(authKey, user.getCustomerKey());

        if (res == null || res.billingKey() == null || res.billingKey().isBlank()) {
            throw new BusinessException(SubscriptionErrorCode.BILLING_KEY_ISSUE_FAILED);
        }

        // 여기에는 "billingKey"만 저장
        s.registerBillingKey(res.billingKey());
    }

    /**
     * 자동결제 취소(다음 결제부터 끊기)
     * - 기간 남아있으면 CANCELED로 변경(권한 유지)
     * - 만료면 EXPIRED로 정리
     * - 이미 CANCELED면 멱등 처리
     */
    @Transactional
    public void cancelAutoPay(Long subscriberId) {
        Subscription s = subscriptionRepository.findBySubscriber_Id(subscriberId)
                .orElseThrow(() -> new BusinessException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));

        LocalDate today = LocalDate.now();

        if (s.getEndDate() != null && s.getEndDate().isBefore(today)) {
            s.expire();
            return;
        }

        if (s.getStatus() == SubscriptionStatus.CANCELED) {
            return; // 멱등
        }

        if (s.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new BusinessException(SubscriptionErrorCode.INVALID_SUBSCRIPTION_STATUS);
        }

        s.cancelAutoPay();
    }

    /**
     * 자동결제 재등록(다시 ON)
     * - 기간 남아있는 CANCELED만 ACTIVE로
     * - 만료면 불가
     * - 이미 ACTIVE면 멱등
     */
    @Transactional
    public void reactivateAutoPay(Long subscriberId) {
        Subscription s = subscriptionRepository.findBySubscriber_Id(subscriberId)
                .orElseThrow(() -> new BusinessException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));

        LocalDate today = LocalDate.now();

        if (s.getEndDate() != null && s.getEndDate().isBefore(today)) {
            throw new BusinessException(SubscriptionErrorCode.CANNOT_REACTIVATE_EXPIRED);
        }

        if (s.getStatus() == SubscriptionStatus.ACTIVE) {
            return;
        }

        if (s.getStatus() != SubscriptionStatus.CANCELED) {
            throw new BusinessException(SubscriptionErrorCode.INVALID_SUBSCRIPTION_STATUS);
        }

        s.activateAutoPay();
    }

    public SubscriptionMeResponse getMySubscription(Long subscriberId) {
        Subscription s = subscriptionRepository.findBySubscriber_Id(subscriberId)
                .orElseThrow(() -> new BusinessException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));

        return SubscriptionMeResponse.from(s);
    }
}
