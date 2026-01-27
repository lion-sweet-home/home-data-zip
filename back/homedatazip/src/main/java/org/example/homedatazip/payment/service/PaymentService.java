package org.example.homedatazip.payment.service;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.exception.BusinessException;
import org.example.homedatazip.global.exception.domain.PaymentErrorCode;
import org.example.homedatazip.payment.client.TossPaymentClient;
import org.example.homedatazip.payment.dto.PaymentApproveRequest;
import org.example.homedatazip.payment.dto.PaymentApproveResponse;
import org.example.homedatazip.payment.entity.PaymentLog;
import org.example.homedatazip.payment.repository.PaymentLogRepository;
import org.example.homedatazip.payment.type.PaymentStatus;
import org.example.homedatazip.subscription.entity.Subscription;
import org.example.homedatazip.subscription.repository.SubscriptionRepository;
import org.example.homedatazip.subscription.type.SubscriptionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentLogRepository paymentLogRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final TossPaymentClient tossPaymentClient;

    /**
     * 프론트가 결제 성공 후 호출하는 "승인 처리"
     * - Toss approve 호출
     * - PaymentLog 저장(APPROVED)
     * - Subscription ACTIVE + 기간 세팅 (첫결제/재구독용)
     */
    @Transactional
    public PaymentApproveResponse approvePayment(Long subscriberId, PaymentApproveRequest request) {

        Subscription subscription = subscriptionRepository.findBySubscriber_Id(subscriberId)
                .orElseThrow(() -> new BusinessException(PaymentErrorCode.SUBSCRIPTION_NOT_FOUND));

        // 중복 방지
        if (paymentLogRepository.existsByOrderId(request.orderId())) {
            throw new BusinessException(PaymentErrorCode.DUPLICATE_ORDER_ID);
        }
        if (paymentLogRepository.existsByPaymentKey(request.paymentKey())) {
            throw new BusinessException(PaymentErrorCode.DUPLICATE_PAYMENT_KEY);
        }

        // (선택) 금액 검증: 기본요금제면 subscription.price 또는 고정값과 비교
        // 지금 subscription에 price가 있으니 비교 가능
        if (subscription.getPrice() != null && !subscription.getPrice().equals(request.amount())) {
            throw new BusinessException(PaymentErrorCode.INVALID_AMOUNT);
        }

        // 1) 토스 승인
        var result = tossPaymentClient.approve(request.paymentKey(), request.orderId(), request.amount());

        // 2) 로그 저장
        PaymentLog log = PaymentLog.builder()
                .subscription(subscription)
                .orderId(result.orderId())
                .paymentKey(result.paymentKey())
                .orderName("구독 결제") // 필요하면 request로 받거나 subscription.name 사용
                .amount(result.amount())
                .paymentStatus(PaymentStatus.APPROVED)
                .approvedAt(result.approvedAt())
                .paidAt(LocalDateTime.now())
                .build();

        paymentLogRepository.save(log);

        // 3) 구독 반영: 첫결제/재구독이면 기간 새로 세팅
        // 정책: 승인되면 오늘부터 1개월
        activateOrRenewSubscription(subscription);

        return new PaymentApproveResponse(
                log.getId(),
                log.getOrderId(),
                log.getAmount(),
                log.getPaymentStatus().name(),
                log.getApprovedAt()
        );
    }

    /**
     * 정기결제 성공 시 호출: 결제 로그 남기고 + 구독 연장(+1개월)
     */
    @Transactional
    public void recordRecurringSuccess(Subscription subscription, String orderId, String orderName, Long amount, LocalDateTime approvedAt) {

        PaymentLog log = PaymentLog.builder()
                .subscription(subscription)
                .orderId(orderId)
                .paymentKey(null) // 빌링 결제도 paymentKey가 나오면 넣고, 아니면 null
                .orderName(orderName)
                .amount(amount)
                .paymentStatus(PaymentStatus.APPROVED)
                .approvedAt(approvedAt)
                .paidAt(LocalDateTime.now())
                .build();

        paymentLogRepository.save(log);

        subscription.extendOneMonth();
    }

    /**
     * 정기결제 실패 시 호출: 실패 로그 남기고 + 구독 자동결제 끊기(CANCELED)
     */
    @Transactional
    public void recordRecurringFailure(Subscription subscription, String orderId, String orderName, Long amount, String reason) {

        PaymentLog log = PaymentLog.builder()
                .subscription(subscription)
                .orderId(orderId)
                .paymentKey(null)
                .orderName(orderName)
                .amount(amount)
                .paymentStatus(PaymentStatus.FAILED)
                .failReason(reason)
                .approvedAt(null)
                .paidAt(null)
                .build();

        paymentLogRepository.save(log);

        // 정책: 실패하면 자동결제 끊기(상태 CANCELED)
        subscription.cancelAutoPay();
    }

    /**
     * 승인 성공 시 구독 상태를 ACTIVE로 맞추고 기간 세팅
     * - 기본 정책: 오늘 ~ +1개월
     * - 이미 ACTIVE인데 기간 남아있으면 막을지/연장할지 정책에 따라 변경 가능
     */
    private void activateOrRenewSubscription(Subscription subscription) {
        LocalDate today = LocalDate.now();

        // 만료면 정리 후 새로 시작
        if (subscription.getEndDate() != null && subscription.getEndDate().isBefore(today)) {
            subscription.expire();
        }

        // 승인 성공 = 무조건 ACTIVE로 전환하고 기간 리셋(단순 정책)
        subscription.activate();
        subscription.resetPeriod(today, today.plusMonths(1));
        subscription.updatePlan("기본 요금제", subscription.getPrice() == null ? 9900L : subscription.getPrice());
    }
}
