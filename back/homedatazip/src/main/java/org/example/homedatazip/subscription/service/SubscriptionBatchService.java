package org.example.homedatazip.subscription.service;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.exception.BusinessException;
import org.example.homedatazip.global.exception.domain.SubscriptionErrorCode;
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

    @Transactional
    public int expire(LocalDate today) {
        if (today == null) {
            throw new BusinessException(SubscriptionErrorCode.BATCH_DATE_REQUIRED);
        }

        LocalDate expireBefore = today.minusDays(1);

        List<Subscription> targets =
                subscriptionRepository.findAllByIsActiveTrueAndEndDateIsNotNullAndStatusInAndEndDateLessThan(
                        List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELED),
                        expireBefore
                );

        for (Subscription s : targets) {
            s.expire();
        }

        return targets.size();
    }
}
