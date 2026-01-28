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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private static final long BASIC_AMOUNT = 9900L;
    private static final String BASIC_ORDER_NAME = "기본 요금제";

    private final TossPaymentClient tossPaymentClient;
    private final PaymentLogRepository paymentLogRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Transactional(readOnly = true)
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
        if (!request.amount().equals(BASIC_AMOUNT)) {
            throw new BusinessException(PaymentErrorCode.INVALID_AMOUNT);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(PaymentErrorCode.USER_NOT_FOUND));

        Subscription sub = subscriptionRepository.findBySubscriber_Id(userId).orElse(null);
        if (sub == null) {
            LocalDate today = LocalDate.now();
            sub = subscriptionRepository.save(Subscription.builder()
                    .subscriber(user)
                    .name(BASIC_ORDER_NAME)
                    .price(BASIC_AMOUNT)
                    .status(SubscriptionStatus.EXPIRED)
                    .isActive(false)
                    .startDate(today)
                    .endDate(today)
                    .build());
        }

        LocalDateTime now = LocalDateTime.now();

        PaymentLog processing;
        try {
            processing = paymentLogRepository.save(PaymentLog.builder()
                    .subscription(sub)
                    .orderId(request.orderId())
                    .paymentKey(request.paymentKey())
                    .orderName(BASIC_ORDER_NAME)
                    .amount(request.amount()) // 일단 요청값 기록(나중에 승인값으로 덮어씀)
                    .paymentStatus(PaymentStatus.PROCESSING)
                    .approvedAt(now)
                    .paidAt(now)
                    .build());
            paymentLogRepository.flush();
        } catch (DataIntegrityViolationException dup) {

            PaymentLog existing = paymentLogRepository.findByPaymentKey(request.paymentKey())
                    .orElseGet(() -> paymentLogRepository.findByOrderId(request.orderId())
                            .orElseThrow(() -> dup));

            // APPROVED면 그대로 성공 응답
            if (existing.getPaymentStatus() == PaymentStatus.APPROVED) {
                return toConfirmResponse(existing);
            }

            // PROCESSING이면 "이미 처리중"
            // 여기서는 그냥 현재 상태 반환(프론트가 재조회/새로고침 하면 됨)
            if (existing.getPaymentStatus() == PaymentStatus.PROCESSING) {
                return toConfirmResponse(existing);
            }

            // FAILED면 다시 결제하라고 던지거나, 정책상 새 orderId로 재시도 유도
            throw new BusinessException(PaymentErrorCode.TOSS_APPROVE_FAILED);
        }

        try {
            TossPaymentConfirmResponse tossRes = tossPaymentClient.approve(
                    request.paymentKey(),
                    request.orderId(),
                    request.amount()
            );

            Long approvedAmount = tossRes.totalAmount();
            if (approvedAmount == null) {
                throw new BusinessException(PaymentErrorCode.TOSS_INVALID_RESPONSE);
            }
            if (!approvedAmount.equals(BASIC_AMOUNT)) {
                throw new BusinessException(PaymentErrorCode.INVALID_AMOUNT);
            }

            processing.markApproved(tossRes.paymentKey(), tossRes.orderId(), approvedAmount, now);
            paymentLogRepository.flush();

            LocalDate today = LocalDate.now();
            LocalDate start;
            LocalDate end;

            if (sub.getEndDate() != null && !sub.getEndDate().isBefore(today)) {
                start = (sub.getStartDate() != null) ? sub.getStartDate() : today;
                end = sub.getEndDate().plusMonths(1);
            } else {
                start = today;
                end = today.plusMonths(1);
            }

            sub.activateAutoPay();
            sub.resetPeriod(start, end);
            subscriptionRepository.flush();

            return new PaymentConfirmResponse(
                    processing.getPaymentKey(),
                    processing.getOrderId(),
                    processing.getAmount(),
                    now,
                    sub.getId(),
                    sub.getStatus(),
                    sub.getStartDate(),
                    sub.getEndDate()
            );

        } catch (Exception e) {
            log.error("[PAYMENT] Toss approve failed. orderId={}, amount={}, paymentKey={}",
                    request.orderId(), request.amount(), request.paymentKey(), e);

            markFailedNewTx(processing.getId(), safeMsg(e));

            sub.expire();
            throw new BusinessException(PaymentErrorCode.TOSS_APPROVE_FAILED);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void saveFailedLogNewTx(Subscription sub, TossPaymentConfirmRequest request, LocalDateTime now, Exception e) {
        paymentLogRepository.save(PaymentLog.builder()
                .subscription(sub)
                .orderId(request.orderId())
                .paymentKey(request.paymentKey())
                .orderName(BASIC_ORDER_NAME)
                .amount(request.amount())
                .paymentStatus(PaymentStatus.FAILED)
                .failReason(safeMsg(e))
                .approvedAt(now)
                .paidAt(now)
                .build());
        paymentLogRepository.flush();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void markFailedNewTx(Long paymentLogId, String reason) {
        PaymentLog log = paymentLogRepository.findById(paymentLogId)
                .orElseThrow();
        log.markFailed(reason);
        paymentLogRepository.flush();
    }

    private PaymentConfirmResponse toConfirmResponse(PaymentLog log) {
        Subscription sub = log.getSubscription();

        return new PaymentConfirmResponse(
                log.getPaymentKey(),
                log.getOrderId(),
                log.getAmount(),
                log.getApprovedAt(),
                sub.getId(),
                sub.getStatus(),
                sub.getStartDate(),
                sub.getEndDate()
        );
    }

    private String generateOrderId() {
        return "ORD_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    }

    private String safeMsg(Exception e) {
        String msg = e.getMessage();
        return (msg == null || msg.isBlank()) ? "결제 실패(원인 미상)" : msg;
    }

    private long resolveApprovedAmount(TossPaymentConfirmResponse res) {
        if (res.totalAmount() != null) return res.totalAmount();
        throw new BusinessException(PaymentErrorCode.TOSS_INVALID_RESPONSE);
    }
}
