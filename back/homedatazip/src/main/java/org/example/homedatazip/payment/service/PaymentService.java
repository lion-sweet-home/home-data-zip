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

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    @PersistenceContext
    private EntityManager em;

    private static final long BASIC_AMOUNT = 9900L;
    private static final String BASIC_ORDER_NAME = "기본 요금제";

    private final TossPaymentClient tossPaymentClient;
    private final PaymentLogRepository paymentLogRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Transactional(readOnly = true)
    public PaymentPrepareResponse prepareOneTimePayment(Long userId, PaymentPrepareRequest request) {
        throw new BusinessException(PaymentErrorCode.INVALID_REQUEST);
    }

    public PaymentConfirmResponse confirmOneTimePayment(Long userId, TossPaymentConfirmRequest request) {
        throw new BusinessException(PaymentErrorCode.INVALID_REQUEST);
    }

    // (현재 코드에 남아있길래 유지) - 너는 지금 processing 방식 쓰니까 사실상 안씀.
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
