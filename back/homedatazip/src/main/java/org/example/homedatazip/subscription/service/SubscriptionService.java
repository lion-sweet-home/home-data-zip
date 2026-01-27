package org.example.homedatazip.subscription.service;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.exception.BusinessException;
import org.example.homedatazip.global.exception.domain.SubscriptionErrorCode;
import org.example.homedatazip.subscription.dto.SubscriptionMeResponse;
import org.example.homedatazip.subscription.entity.Subscription;
import org.example.homedatazip.subscription.repository.SubscriptionRepository;
import org.example.homedatazip.subscription.type.SubscriptionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

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

    @Transactional(readOnly = true)
    public SubscriptionMeResponse getMySubscription(Long subscriberId) {
        Subscription s = subscriptionRepository.findBySubscriber_Id(subscriberId)
                .orElseThrow(() -> new BusinessException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));

        return SubscriptionMeResponse.from(s);
    }
}
