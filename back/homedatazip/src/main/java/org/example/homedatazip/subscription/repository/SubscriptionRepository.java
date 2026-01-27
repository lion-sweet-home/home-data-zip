package org.example.homedatazip.subscription.repository;

import org.example.homedatazip.subscription.entity.Subscription;
import org.example.homedatazip.subscription.type.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    // 구독자 구독 조회
    Optional<Subscription> findBySubscriberId(Long subscriberId);

    boolean existsBySubscriberId(Long subscriberId);

    // 만료 처리 스케줄러용: EXPIRED 변환 대상 리스트 조회
    // isActive=true인데 endDate < today면 EXPIRED로 전환
    List<Subscription> findAllByIsActiveTrueAndEndDateBefore(LocalDate today);

    // 정기결제 대상 리스트 조회
    // endDate가 오늘인 애들만 뽑고 싶으면 LessThanEqual 사용
    /*isActive = true
    status = :status
    endDate <= :today*/
    List<Subscription> findAllByIsActiveTrueAndStatusAndEndDateLessThanEqual(
            SubscriptionStatus status,
            LocalDate today
    );

    // 취소 API에서 본인 구독인지 확인할 때
    Optional<Subscription> findByIdAndSubscriberId(Long id, Long subscriberId);

    boolean existsByIsActiveTrueAndStatus(SubscriptionStatus status);

}