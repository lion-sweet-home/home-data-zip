package org.example.homedatazip.payment.service;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.exception.BusinessException;
import org.example.homedatazip.global.exception.domain.PaymentErrorCode;
import org.example.homedatazip.payment.dto.PaymentListResponse;
import org.example.homedatazip.payment.dto.PaymentLogResponse;
import org.example.homedatazip.payment.entity.PaymentLog;
import org.example.homedatazip.payment.repository.PaymentLogRepository;
import org.example.homedatazip.subscription.entity.Subscription;
import org.example.homedatazip.subscription.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentQueryService {

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentLogRepository paymentLogRepository;

    public PaymentListResponse getMyPayments(Long userId) {
        Subscription sub = subscriptionRepository.findBySubscriber_Id(userId)
                .orElseThrow(() -> new BusinessException(PaymentErrorCode.SUBSCRIPTION_NOT_FOUND));

        List<PaymentLogResponse> list = paymentLogRepository
                .findAllBySubscription_IdOrderByPaidAtDesc(sub.getId())
                .stream()
                .map(PaymentLogResponse::from)
                .toList();

        return new PaymentListResponse(sub.getId(), list);
    }

    public PaymentLogResponse getMyLatestPayment(Long userId) {
        Subscription sub = subscriptionRepository.findBySubscriber_Id(userId)
                .orElseThrow(() -> new BusinessException(PaymentErrorCode.SUBSCRIPTION_NOT_FOUND));

        PaymentLog log = paymentLogRepository.findTop1BySubscription_IdOrderByPaidAtDesc(sub.getId())
                .orElseThrow(() -> new BusinessException(PaymentErrorCode.PAYMENT_LOG_NOT_FOUND));

        return PaymentLogResponse.from(log);
    }
}
