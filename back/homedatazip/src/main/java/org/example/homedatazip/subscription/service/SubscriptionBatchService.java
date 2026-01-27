package org.example.homedatazip.subscription.service;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.subscription.entity.Subscription;
import org.example.homedatazip.subscription.repository.SubscriptionRepository;
import org.example.homedatazip.subscription.type.SubscriptionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionBatchService {

    private final SubscriptionRepository subscriptionRepository;

    /**
     * 만료 처리
     * - isActive=true
     * - status in (ACTIVE, CANCELED)
     * - endDate < today
     */
    @Transactional
    public int expire(LocalDate today) {

        List<Subscription> targets =
                subscriptionRepository.findAllByIsActiveTrueAndStatusInAndEndDateLessThan(
                        EnumSet.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELED),
                        today
                );

        for (Subscription s : targets) {
            s.expire();
        }

        return targets.size();
    }

    /**
     * 정기결제 대상 뽑기(구독 도메인 기준)
     * - 실제 결제 호출은 PaymentBatchService로 넘어갈 예정이니
     * - 여기서는 "대상 조회" 정도만 두거나, 임시로만 유지해도 됨
     *
     * 지금은 일단 "billingKey 없으면 자동결제 불가 => CANCELED" 같은 최소 방어만 해둠.
     */
    @Transactional
    public int processRecurringPayment(LocalDate today) {

        List<Subscription> targets =
                subscriptionRepository.findAllByIsActiveTrueAndStatusAndEndDateEqual(
                        SubscriptionStatus.ACTIVE,
                        today
                );

        int successCount = 0;

        for (Subscription s : targets) {
            // billingKey 없으면 결제 자체가 불가능하니 자동결제 OFF로 전환
            if (!s.hasBillingKey()) {
                s.cancelAutoPay(); // 권한은 오늘까지 유지
                continue;
            }

            try {
                // TODO: PaymentBatchService에서 토스 정기결제 호출 후
                // 성공이면 extendOneMonth(), 실패면 cancelAutoPay() 하도록 위임 예정

                boolean paid = true; // 임시

                if (paid) {
                    s.extendOneMonth();
                    successCount++;
                } else {
                    s.cancelAutoPay();
                }
            } catch (Exception e) {
                s.cancelAutoPay();
            }
        }

        return successCount;
    }
}
