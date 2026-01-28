package org.example.homedatazip.subscription.service;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.exception.BusinessException;
import org.example.homedatazip.global.exception.domain.SubscriptionErrorCode;
import org.example.homedatazip.subscription.dto.BillingAuthSuccessRequest;
import org.example.homedatazip.subscription.dto.BillingKeyIssueRequest;
import org.example.homedatazip.subscription.dto.BillingKeyIssueResponse;
import org.example.homedatazip.subscription.entity.Subscription;
import org.example.homedatazip.subscription.repository.SubscriptionRepository;
import org.example.homedatazip.user.entity.User;
import org.example.homedatazip.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BillingService {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;

    /**
     * 카드 등록 시작용 정보 발급
     * - 프론트가 Toss SDK requestBillingAuth 호출할 때 customerKey 필요
     * - 우리는 User.getCustomerKey()로 만든다고 했으니 그걸 내려줌
     *
     * ✅ 여기서 Subscription 레코드 미리 만들지 않는다.
     */
    public BillingKeyIssueResponse issue(Long subscriberId, BillingKeyIssueRequest request) {

        User user = userRepository.findById(subscriberId)
                .orElseThrow(() -> new BusinessException(SubscriptionErrorCode.SUBSCRIBER_NOT_FOUND));

        return new BillingKeyIssueResponse(
                user.getCustomerKey(),
                request.orderName(),
                request.amount()
        );
    }

    /**
     * 카드 등록 성공 -> billingKey 저장
     * ✅ billingKey 저장할 곳은 Subscription이므로, 구독 레코드가 있어야 함.
     * - 없으면 "구독 먼저 시작하세요" 정책으로 막는다.
     *   (원하면 여기서 자동 생성도 가능하지만 너는 간결/명확 선호라 막는 게 깔끔)
     */
    @Transactional
    public void onAuthSuccess(Long subscriberId, BillingAuthSuccessRequest request) {

        User user = userRepository.findById(subscriberId)
                .orElseThrow(() -> new BusinessException(SubscriptionErrorCode.SUBSCRIBER_NOT_FOUND));

        if (request.customerKey() == null || !request.customerKey().equals(user.getCustomerKey())) {
            throw new BusinessException(SubscriptionErrorCode.INVALID_CUSTOMER_KEY);
        }

        Subscription s = subscriptionRepository.findBySubscriber_Id(subscriberId)
                .orElseThrow(() -> new BusinessException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));

        if (request.authKey() == null || request.authKey().isBlank()) {
            throw new BusinessException(SubscriptionErrorCode.BILLING_KEY_REQUIRED);
        }

        s.registerBillingKey(request.authKey()); // 엔티티에 있어야 함
    }
}
