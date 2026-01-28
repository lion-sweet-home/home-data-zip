package org.example.homedatazip.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.global.exception.BusinessException;
import org.example.homedatazip.global.exception.domain.PaymentErrorCode;
import org.example.homedatazip.payment.client.TossPaymentClient;
import org.example.homedatazip.payment.dto.*;
import org.example.homedatazip.payment.entity.PaymentLog;
import org.example.homedatazip.payment.repository.PaymentLogRepository;
import org.example.homedatazip.payment.type.PaymentStatus;
import org.example.homedatazip.subscription.entity.Subscription;
import org.example.homedatazip.subscription.repository.SubscriptionRepository;
import org.example.homedatazip.subscription.type.SubscriptionStatus;
import org.example.homedatazip.user.entity.User;
import org.example.homedatazip.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private static final long BASIC_AMOUNT = 9900L;
    private static final String BASIC_ORDER_NAME = "기본 요금제";

    private final TossPaymentClient tossPaymentClient;
    private final PaymentLogRepository paymentLogRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;

    /**
     * ✅ 결제창 열기 전 prepare
     * - 정책: 기본요금제만
     * - orderId 생성 + customerKey 내려줌
     */
    @Transactional
    public PaymentPrepareResponse prepareOneTimePayment(Long userId, PaymentPrepareRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(PaymentErrorCode.USER_NOT_FOUND));

        return new PaymentPrepareResponse(
                user.getCustomerKey(),
                generateOrderId(),
                BASIC_ORDER_NAME,
                BASIC_AMOUNT
        );
    }

    /**
     * ✅ 단건결제 승인(confirm)
     * - 성공: PaymentLog(APPROVED) + 구독 ACTIVE + "무조건 1개월 연장"
     * - 실패: PaymentLog(FAILED) + 구독 EXPIRED
     */
    @Transactional
    public PaymentConfirmResponse confirmOneTimePayment(Long userId, TossPaymentConfirmRequest request) {

        if (request.paymentKey() == null || request.paymentKey().isBlank()) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_KEY_REQUIRED);
        }
        if (request.orderId() == null || request.orderId().isBlank()) {
            throw new BusinessException(PaymentErrorCode.ORDER_ID_REQUIRED);
        }
        if (request.amount() == null || request.amount() <= 0) {
            throw new BusinessException(PaymentErrorCode.INVALID_AMOUNT);
        }

        // ✅ 정책: 기본 금액만 허용(변조 방지)
        if (!request.amount().equals(BASIC_AMOUNT)) {
            throw new BusinessException(PaymentErrorCode.INVALID_AMOUNT);
        }

        // ✅ 중복 confirm 방지 (너가 3번 안해도 된다 했는데, 이건 강추라 남김)
        if (paymentLogRepository.existsByOrderId(request.orderId())) {
            throw new BusinessException(PaymentErrorCode.DUPLICATE_ORDER_ID);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(PaymentErrorCode.USER_NOT_FOUND));

        Subscription sub = subscriptionRepository.findBySubscriber_Id(userId).orElse(null);

        // 구독 레코드 없으면 만들어둠(결제 실패하면 EXPIRED로 끝)
        if (sub == null) {
            LocalDate today = LocalDate.now();
            sub = Subscription.builder()
                    .subscriber(user)
                    .name(BASIC_ORDER_NAME)
                    .price(BASIC_AMOUNT)
                    .status(SubscriptionStatus.EXPIRED)
                    .isActive(false)
                    .startDate(today)
                    .endDate(today)
                    .build();
            sub = subscriptionRepository.save(sub);
        }

        LocalDateTime now = LocalDateTime.now();

        try {
            // ✅ 토스 승인 호출
            TossPaymentConfirmResponse tossRes = tossPaymentClient.approve(
                    request.paymentKey(),
                    request.orderId(),
                    request.amount()
            );

            // ✅ 승인 로그 저장
            paymentLogRepository.save(PaymentLog.builder()
                    .subscription(sub)
                    .orderId(tossRes.orderId())
                    .paymentKey(tossRes.paymentKey())
                    .orderName(BASIC_ORDER_NAME)
                    .amount(tossRes.amount())
                    .paymentStatus(PaymentStatus.APPROVED)
                    .approvedAt(tossRes.approvedAt() != null ? tossRes.approvedAt() : now)
                    .paidAt(now)
                    .build());

            // ✅ 구독 기간 처리: "무조건 1개월 연장"
            LocalDate today = LocalDate.now();
            LocalDate start;
            LocalDate end;

            // 기간 남아있으면 endDate 기준으로 +1개월, 없으면 오늘부터 1개월
            if (sub.getEndDate() != null && !sub.getEndDate().isBefore(today)) {
                start = (sub.getStartDate() != null) ? sub.getStartDate() : today;
                end = sub.getEndDate().plusMonths(1);
            } else {
                start = today;
                end = today.plusMonths(1);
            }

            sub.activateAutoPay();  // ACTIVE + isActive=true
            sub.resetPeriod(start, end);

            return new PaymentConfirmResponse(
                    tossRes.paymentKey(),
                    tossRes.orderId(),
                    tossRes.amount(),
                    tossRes.approvedAt() != null ? tossRes.approvedAt() : now,
                    sub.getId(),
                    sub.getStatus(),
                    sub.getStartDate(),
                    sub.getEndDate()
            );

        } catch (Exception e) {
            paymentLogRepository.save(PaymentLog.builder()
                    .subscription(sub)
                    .orderId(request.orderId())
                    .paymentKey(request.paymentKey())
                    .orderName(BASIC_ORDER_NAME)
                    .amount(request.amount())
                    .paymentStatus(PaymentStatus.FAILED)
                    .failReason(safeMsg(e))
                    .paidAt(now)
                    .build());

            sub.expire();
            throw new BusinessException(PaymentErrorCode.TOSS_APPROVE_FAILED);
        }
    }

    private String generateOrderId() {
        return "ORD_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    }

    private String safeMsg(Exception e) {
        String msg = e.getMessage();
        return (msg == null || msg.isBlank()) ? "결제 실패(원인 미상)" : msg;
    }
}
