package org.example.homedatazip.subscription.service;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.exception.BusinessException;
import org.example.homedatazip.global.exception.domain.SubscriptionErrorCode;
import org.example.homedatazip.role.RoleType;
import org.example.homedatazip.role.repository.RoleRepository;
import org.example.homedatazip.subscription.entity.Subscription;
import org.example.homedatazip.subscription.repository.SubscriptionRepository;
import org.example.homedatazip.subscription.type.SubscriptionStatus;
import org.example.homedatazip.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionBatchService {

    private final SubscriptionRepository subscriptionRepository;
    private final RoleRepository roleRepository;

    /**
     * 만료 처리 (EXPIRED)
     * - ACTIVE/CANCELED 이면서 endDate < (today-1) 인 구독들을 EXPIRED 처리
     */
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

        // SELLER 역할 존재 확인
        roleRepository.findByRoleType(RoleType.SELLER)
                .orElseThrow(() -> new BusinessException(SubscriptionErrorCode.ROLE_NOT_FOUND));

        for (Subscription s : targets) {
            s.expire();

            // SELLER 회수
            User user = s.getSubscriber();
            user.removeRole(RoleType.SELLER); // User에 이 메서드가 있어야 함
        }

        return targets.size();
    }
}
