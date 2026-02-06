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
     */
    @Transactional
    public int expire(LocalDate today) {
        if (today == null) {
            throw new BusinessException(SubscriptionErrorCode.BATCH_DATE_REQUIRED);
        }

        // SELLER 역할 확인
        roleRepository.findByRoleType(RoleType.SELLER)
                .orElseThrow(() -> new BusinessException(SubscriptionErrorCode.ROLE_NOT_FOUND));


        List<Subscription> targets =
                subscriptionRepository.findAllByIsActiveTrueAndEndDateIsNotNullAndStatusInAndEndDateLessThan(
                        List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELED),
                        today
                );

        for (Subscription s : targets) {
            s.expire(); // status=EXPIRED, isActive=false

            // SELLER 회수
            User user = s.getSubscriber();
            user.removeRole(RoleType.SELLER);
        }

        return targets.size();
    }
}
