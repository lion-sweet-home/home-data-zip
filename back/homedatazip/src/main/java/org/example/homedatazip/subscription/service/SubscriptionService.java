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


    // 첫구독 / 재구독
    @Transactional
    public SubscriptionStartResponse startSubscription(Long subscriberId, String name, Long price, int periodDays) {
        LocalDate today = LocalDate.now();

        User subscriber = userRepository.findById(subscriberId)
                .orElseThrow(() -> new BusinessException(SubscriptionErrorCode.SUBSCRIBER_NOT_FOUND));

        Subscription s = subscriptionRepository.findBySubscriber_Id(subscriberId).orElse(null);

        // 1) 레코드 없으면 생성(바로 ACTIVE)
        if (s == null) {
            Subscription created = Subscription.builder()
                    .subscriber(subscriber)
                    .name(name)
                    .price(price)
                    .status(SubscriptionStatus.ACTIVE)
                    .isActive(true)
                    .startDate(today)
                    .endDate(today.plusDays(periodDays))
                    .build();

            Subscription saved = subscriptionRepository.save(created);
            return SubscriptionStartResponse.from(saved);
        }

        // 만료면 EXPIRED로 정리 후 재구독 처리
        if (s.getEndDate() != null && s.getEndDate().isBefore(today)) {
            s.expire(); // status=EXPIRED, isActive=false
        }

        boolean hasRemainingPeriod =
                s.getEndDate() != null && !s.getEndDate().isBefore(today);

        // 기간 남아있는 ACTIVE면 막기(이미 자동결제 ON 상태)
        if (s.getStatus() == SubscriptionStatus.ACTIVE && s.isActive() && hasRemainingPeriod) {
            throw new BusinessException(SubscriptionErrorCode.ALREADY_SUBSCRIBED);
        }

        // 기간 남아있는 CANCELED면 허용 -> 자동결제 다시 ON (ACTIVE로 전환)
        s.activateAutoPay();
        s.updatePlan(name, price);
        s.resetPeriod(today, today.plusDays(periodDays));

        return SubscriptionStartResponse.from(s);
    }


    //자동결제 취소(다음 결제부터 끊기)
    @Transactional
    public void cancelAutoPay(Long subscriberId) {
        Subscription s = subscriptionRepository.findBySubscriber_Id(subscriberId)
                .orElseThrow(() -> new BusinessException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));

        LocalDate today = LocalDate.now();

        // 만료면 그냥 만료로 정리
        if (s.getEndDate() != null && s.getEndDate().isBefore(today)) {
            s.expire();
            return;
        }

        // ACTIVE 또는 CANCELED 모두 허용(멱등)
        if (s.getStatus() == SubscriptionStatus.CANCELED) {
            return;
        }

        if (s.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new BusinessException(SubscriptionErrorCode.INVALID_SUBSCRIPTION_STATUS);
        }

        s.cancelAutoPay(); // status=CANCELED (권한은 유지)
    }


     // 자동결제 재등록
     //- CANCELED -> ACTIVE (기간 남아있을 때만)
     //- 만료면 재등록 불가(새 구독 startSubscription 필요)
    @Transactional
    public void reactivateAutoPay(Long subscriberId) {
        Subscription s = subscriptionRepository.findBySubscriber_Id(subscriberId)
                .orElseThrow(() -> new BusinessException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));

        LocalDate today = LocalDate.now();

        // 만료면 재등록 불가
        if (s.getEndDate() != null && s.getEndDate().isBefore(today)) {
            throw new BusinessException(SubscriptionErrorCode.CANNOT_REACTIVATE_EXPIRED);
        }

        // 기간 남아있는 상태에서만 의미 있음
        if (s.getEndDate() == null || s.getEndDate().isBefore(today)) {
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

    public SubscriptionStartResponse getMySubscription(Long subscriberId) {
        Subscription s = subscriptionRepository.findBySubscriber_Id(subscriberId)
                .orElseThrow(() -> new BusinessException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));

        return SubscriptionStartResponse.from(s);
    }
}
