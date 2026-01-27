package org.example.homedatazip.subscription.service;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.subscription.entity.Subscription;
import org.example.homedatazip.subscription.repository.SubscriptionRepository;
import org.example.homedatazip.subscription.type.SubscriptionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionBatchService {

    private final SubscriptionRepository subscriptionRepository;


    // 만료 처리:
    @Transactional
    public int expire(LocalDate today) {

        List<Subscription> targets =
                subscriptionRepository.findAllByIsActiveTrueAndStatusInAndEndDateLessThan(
                        List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELED),
                        today
                );

        for (Subscription s : targets) {
            s.expire(); // status=EXPIRED, isActive=false
        }

        return targets.size();
    }


    // 정기결제 처리:
    @Transactional
    public int processRecurringPayment(LocalDate today) {

        List<Subscription> targets =
                subscriptionRepository.findAllByIsActiveTrueAndStatusAndEndDateEqual(
                        SubscriptionStatus.ACTIVE,
                        today
                );

        int successCount = 0;

        for (Subscription s : targets) {
            try {
                // TODO: 토스 정기결제 API 호출해서 결제 승인/성공 확인
                boolean paid = true; // payment 연동 후 변경 예정

                if (paid) {
                    // 성공: 기간 연장
                    s.extendOneMonth();
                    successCount++;
                } else {
                    // 실패: 자동결제 중단(권한은 오늘까지 유지)
                    s.cancelAutoPay(); // status=CANCELED
                }
            } catch (Exception e) {
                // 예외도 결제 실패로 보고 자동결제 중단
                s.cancelAutoPay();
            }
        }

        return successCount;
    }
}
