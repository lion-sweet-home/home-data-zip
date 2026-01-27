package org.example.homedatazip.subscription.repository;

import org.example.homedatazip.subscription.entity.Subscription;
import org.example.homedatazip.subscription.type.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    // 유저당 1개 구독 조회
    Optional<Subscription> findBySubscriber_Id(Long subscriberId);

    // 유저가 구독 레코드를 가지고 있는지
    boolean existsBySubscriber_Id(Long subscriberId);

    // (보안용) 본인 구독인지 확인하며 조회
    Optional<Subscription> findByIdAndSubscriber_Id(Long id, Long subscriberId);

    // ---- 배치/스케줄러용 ----

    /**
     * 만료 처리 대상:
     * - 권한이 남아있던 상태(ACTIVE/CANCELED)
     * - isActive=true
     * - endDate < today
     */
    List<Subscription> findAllByIsActiveTrueAndStatusInAndEndDateLessThan(
            List<SubscriptionStatus> statuses,
            LocalDate today
    );

    /**
     * 정기결제 대상:
     * - 자동결제 ON 상태(ACTIVE)만
     * - isActive=true
     * - endDate == today
     */
    List<Subscription> findAllByIsActiveTrueAndStatusAndEndDateEqual(
            SubscriptionStatus status,
            LocalDate endDate
    );
}
