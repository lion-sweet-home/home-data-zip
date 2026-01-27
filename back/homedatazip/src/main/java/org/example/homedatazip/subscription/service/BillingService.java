package org.example.homedatazip.subscription.service;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.exception.BusinessException;
import org.example.homedatazip.global.exception.domain.SubscriptionErrorCode;
import org.example.homedatazip.subscription.dto.BillingAuthFailRequest;
import org.example.homedatazip.subscription.dto.BillingAuthSuccessRequest;
import org.example.homedatazip.subscription.dto.BillingKeyIssueRequest;
import org.example.homedatazip.subscription.dto.BillingKeyIssueResponse;
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
public class BillingService {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;

    /**
     * 카드 등록 시작용 정보 발급
     * - 프론트가 Toss SDK requestBillingAuth를 호출할 때 customerKey가 필요함
     * - 너희는 User.getCustomerKey()로 만든다고 했으니 그 값을 내려줌
     *
     * 정책:
     * - 구독 레코드가 없으면 만들어 두는 게 편함(나중에 billingKey 저장할 곳이 생김)
     * - 단, 여기서는 '결제'가 아니라 '카드 등록' 준비 단계라 권한/기간은 최소값으로 둠
     */
    @Transactional
    public BillingKeyIssueResponse issue(Long subscriberId, BillingKeyIssueRequest request) {

        User user = userRepository.findById(subscriberId)
                .orElseThrow(() -> new BusinessException(SubscriptionErrorCode.SUBSCRIBER_NOT_FOUND));

        // 구독 레코드 없으면 생성(카드등록만 먼저 하는 케이스 대비)
        Subscription s = subscriptionRepository.findBySubscriber_Id(subscriberId).orElse(null);

        if (s == null) {
            LocalDate today = LocalDate.now();
            s = Subscription.builder()
                    .subscriber(user)
                    .name("기본 요금제")
                    .price(9900L)
                    .status(SubscriptionStatus.CANCELED) // 권한 주는 단계 아님, 자동결제 OFF 상태로 시작
                    .isActive(true)
                    .startDate(today)
                    .endDate(today) // 아직 구독 시작 전이니 임시값(권한 체크는 hasAccess로 걸러짐)
                    .build();

            subscriptionRepository.save(s);
        }

        // customerKey는 User에서 만든다 했으니 이걸 프론트로 내려줌
        String customerKey = user.getCustomerKey();

        // orderName/amount는 카드등록 단계에서는 실제 결제금액이 아니라 “표시용”으로 쓰는 경우가 많음
        return new BillingKeyIssueResponse(
                customerKey,
                request.orderName(),
                request.amount()
        );
    }

    /**
     * 카드 등록 성공 콜백 처리
     * - 프론트(또는 리디렉트 받은 페이지)가 서버로 customerKey/authKey를 전달
     * - 여기서 authKey(또는 billingKey)를 Subscription.billingKey에 저장
     */
    @Transactional
    public void onAuthSuccess(Long subscriberId, BillingAuthSuccessRequest request) {

        User user = userRepository.findById(subscriberId)
                .orElseThrow(() -> new BusinessException(SubscriptionErrorCode.SUBSCRIBER_NOT_FOUND));

        // customerKey 검증(남의 customerKey로 공격 방지)
        String expectedCustomerKey = user.getCustomerKey();
        if (request.customerKey() == null || !request.customerKey().equals(expectedCustomerKey)) {
            throw new BusinessException(SubscriptionErrorCode.INVALID_CUSTOMER_KEY);
        }

        Subscription s = subscriptionRepository.findBySubscriber_Id(subscriberId)
                .orElseThrow(() -> new BusinessException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));

        // authKey를 billingKey로 저장(※ 실제 필드명이 billingKey로 오면 그때 DTO만 바꾸면 됨)
        if (request.authKey() == null || request.authKey().isBlank()) {
            throw new BusinessException(SubscriptionErrorCode.BILLING_KEY_REQUIRED);
        }

        s.registerBillingKey(request.authKey());

        // 카드 등록 성공하면 자동결제 ON으로 돌려도 됨(정책 선택)
        // "결제 전"인데 ACTIVE로 바꾸는 게 부담이면 이 줄은 빼고, startSubscription 성공 시 ACTIVE로 바꾸면 됨.
        s.activateAutoPay();
    }

    /**
     * 카드 등록 실패 콜백(로그용)
     * - 지금은 DB 로그 테이블 없으니 최소로만 처리
     * - 필요하면 나중에 BillingAuthLog 엔티티 만들면 됨
     */
    @Transactional
    public void onAuthFail(Long subscriberId, BillingAuthFailRequest request) {

        // 실패했다고 해서 구독을 망가뜨릴 필요는 없음
        // 다만 기존 billingKey가 꼬여있을 수 있으니 "카드등록 실패 시 billingKey를 지울지"는 정책임.
        // 안전하게는 지우지 않는 게 맞음(기존 카드가 있을 수 있음).

        // 원하면 아래처럼 옵션으로 처리 가능:
        // Subscription s = subscriptionRepository.findBySubscriber_Id(subscriberId).orElse(null);
        // if (s != null) { s.cancelAutoPay(); }
    }

    /**
     * (옵션) 카드 삭제/초기화
     * - billingKey를 제거하고 자동결제 OFF로 돌림
     */
    @Transactional
    public void clearBillingKey(Long subscriberId) {
        Subscription s = subscriptionRepository.findBySubscriber_Id(subscriberId)
                .orElseThrow(() -> new BusinessException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));

        s.cancelAutoPay();
        s.clearBillingKey();
    }
}
