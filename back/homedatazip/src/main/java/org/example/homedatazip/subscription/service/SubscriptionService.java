package org.example.homedatazip.subscription.service;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.exception.BusinessException;
import org.example.homedatazip.global.exception.domain.SubscriptionErrorCode;
import org.example.homedatazip.subscription.dto.SubscriptionStartResponse;
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

    /**
     * 첫구독 / 재구독(만료 후 다시 시작)
     * - 이미 기간 남은 ACTIVE면 막기
     * - 기간 남은 CANCELED면 "자동결제 재개"로 처리(기간 리셋 X)
     */
    @Transactional
    public SubscriptionStartResponse startSubscription(Long subscriberId, String name, Long price, int periodDays) {
        LocalDate today = LocalDate.now();

        User subscriber = userRepository.findById(subscriberId)
                .orElseThrow(() -> new BusinessException(SubscriptionErrorCode.SUBSCRIBER_NOT_FOUND));

        Subscription s = subscriptionRepository.findBySubscriber_Id(subscriberId).orElse(null);

        LocalDate newEndDate = today.plusDays(periodDays);

        // 1) 레코드 없으면 생성
        if (s == null) {
            Subscription created = Subscription.builder()
                    .subscriber(subscriber)
                    .name(name)
                    .price(price)
                    .status(SubscriptionStatus.ACTIVE)
                    .isActive(true)
                    .startDate(today)
                    .endDate(newEndDate)
                    .build();

            return SubscriptionStartResponse.from(subscriptionRepository.save(created));
        }

        // 2) 만료 여부 계산 (endDate < today)
        boolean isExpiredByDate = s.getEndDate() != null && s.getEndDate().isBefore(today);
        boolean hasRemainingPeriod = s.getEndDate() != null && !s.getEndDate().isBefore(today);

        // 2-1) 날짜상 만료면 정리
        if (isExpiredByDate) {
            s.expire(); // EXPIRED + isActive=false
        }

        // 3) 상태별 처리
        // 3-1) 기간 남은 ACTIVE면 막기
        if (s.getStatus() == SubscriptionStatus.ACTIVE && s.isActive() && hasRemainingPeriod) {
            throw new BusinessException(SubscriptionErrorCode.ALREADY_SUBSCRIBED);
        }

        // 3-2) 기간 남은 CANCELED면: 재구독이 아니라 자동결제만 다시 ON (기간 리셋 X)
        if (s.getStatus() == SubscriptionStatus.CANCELED && hasRemainingPeriod) {
            s.activateAutoPay();
            return SubscriptionStartResponse.from(s);
        }

        // 3-3) 그 외(EXPIRED이거나, 기간이 끝났던 케이스) => 새로 시작(기간 리셋)
        s.activateAutoPay();
        s.updatePlan(name, price);
        s.resetPeriod(today, newEndDate);

        return SubscriptionStartResponse.from(s);
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

        // 만료면 불가
        if (s.getEndDate() != null && s.getEndDate().isBefore(today)) {
            throw new BusinessException(SubscriptionErrorCode.CANNOT_REACTIVATE_EXPIRED);
        }

        // 이미 ACTIVE면 OK
        if (s.getStatus() == SubscriptionStatus.ACTIVE) {
            return;
        }

        // CANCELED만 재등록 가능
        if (s.getStatus() != SubscriptionStatus.CANCELED) {
            throw new BusinessException(SubscriptionErrorCode.INVALID_SUBSCRIPTION_STATUS);
        }

        s.activateAutoPay();
    }

    @Transactional(readOnly = true)
    public SubscriptionStartResponse getMySubscription(Long subscriberId) {
        Subscription s = subscriptionRepository.findBySubscriber_Id(subscriberId)
                .orElseThrow(() -> new BusinessException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));

        return SubscriptionStartResponse.from(s);
    }
}
