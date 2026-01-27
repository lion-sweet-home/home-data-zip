package org.example.homedatazip.subscription.service;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.subscription.dto.SubscriptionStartResponse;
import org.example.homedatazip.subscription.entity.Subscription;
import org.example.homedatazip.subscription.repository.SubscriptionRepository;
import org.example.homedatazip.subscription.type.SubscriptionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    /**
     * 구독 시작
     * - 시작 전에 만료 정리 한번 돌림(데이터 꼬임 방지)
     * - 이미 ACTIVE(자동결제 ON)인 구독이 있으면 막음
     * - CANCELLED(자동결제 OFF)인데 아직 기간 남아있는 경우는 "재활성화"로 처리하는 게 일반적이라
     *   startSubscription 대신 reactivateAutoPay()를 쓰는 걸 추천
     */
    public SubscriptionStartResponse startSubscription(String name, Long price, int periodDays) {
        LocalDate today = LocalDate.now();

        // 혹시 만료처리가 누락된 데이터가 있으면 먼저 정리
        expireSubscriptions(today);

        // 이미 자동결제 ON(ACTIVE)인 구독이 있으면 새로 시작 못 하게 막기
        if (subscriptionRepository.existsByIsActiveTrueAndStatus(SubscriptionStatus.ACTIVE)) {
            throw new IllegalStateException("이미 구독 중입니다.");
        }

        LocalDate endDate = today.plusDays(periodDays);

        Subscription subscription = Subscription.builder()
                .name(name)
                .price(price)
                .status(SubscriptionStatus.ACTIVE)
                .isActive(true)
                .startDate(today)
                .endDate(endDate)
                .build();

        Subscription saved = subscriptionRepository.save(subscription);
        return SubscriptionStartResponse.from(saved);
    }

    /**
     * 자동결제 취소(다음 결제 끊기)
     * - 아직 기간 남아있어도 status만 CANCELLED로 바꿈
     * - 현재 이용권 자체는 살아있을 수 있으니 isActive는 true 유지
     */
    public void cancelAutoPay(Long subscriptionId) {
        Subscription s = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("구독이 없습니다. id=" + subscriptionId));

        LocalDate today = LocalDate.now();

        // 이미 만료면 만료로 정리
        if (s.getEndDate() != null && s.getEndDate().isBefore(today)) {
            s.expire(); // status=EXPIRED, isActive=false
            return;
        }

        // ACTIVE든 CANCELLED든 결과는 "다음 결제 OFF"
        s.cancelAutoPay(); // status=CANCELLED, isActive=true 유지
    }

    /**
     * 자동결제 재등록(다시 ON)
     * - CANCELLED(다음 결제 OFF)였던 구독을 다시 ACTIVE로 되돌림
     * - 단, 이미 만료(EXPIRED)된 건 재활성화가 아니라 "새 구독 시작"으로 처리해야 맞음
     */
    public void reactivateAutoPay(Long subscriptionId) {
        Subscription s = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("구독이 없습니다. id=" + subscriptionId));

        LocalDate today = LocalDate.now();

        // 만료면 되돌리기 금지: 새 구독 start로 유도
        if (s.getEndDate() != null && s.getEndDate().isBefore(today)) {
            s.expire();
            throw new IllegalStateException("이미 만료된 구독입니다. 새로 구독을 시작하세요.");
        }

        // 이미 ACTIVE면 멱등 처리(그냥 성공으로 끝)
        if (s.getStatus() == SubscriptionStatus.ACTIVE) {
            return;
        }

        // CANCELLED -> ACTIVE
        s.activate(); // status=ACTIVE, isActive=true
    }

    /**
     * 만료 처리
     * - isActive=true AND status=ACTIVE AND endDate<=today 인 것들 찾아서 EXPIRED 처리
     */
    public int expireSubscriptions(LocalDate today) {
        List<Subscription> targets =
                subscriptionRepository.findAllByIsActiveTrueAndStatusAndEndDateLessThanEqual(
                        SubscriptionStatus.ACTIVE,
                        today
                );

        for (Subscription s : targets) {
            s.expire(); // status=EXPIRED, isActive=false
        }

        return targets.size();
    }

    /**
     * 단건 조회
     */
    @Transactional(readOnly = true)
    public SubscriptionStartResponse getSubscription(Long subscriptionId) {
        Subscription s = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("구독이 없습니다. id=" + subscriptionId));

        return SubscriptionStartResponse.from(s);
    }
}
