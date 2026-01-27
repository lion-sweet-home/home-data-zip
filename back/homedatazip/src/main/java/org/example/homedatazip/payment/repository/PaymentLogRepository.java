package org.example.homedatazip.payment.repository;

import org.example.homedatazip.payment.entity.PaymentLog;
import org.example.homedatazip.payment.type.PaymentStatus;
import org.example.homedatazip.subscription.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PaymentLogRepository extends JpaRepository<PaymentLog, Long> {

    @Query("""
            select coalesce(sum(p.amount), 0) from PaymentLog p
            where date(p.paidAt) = :date
            and p.paymentStatus = :status
            """)
    Long sumAmountByPaidDate(
            @Param("date") LocalDate paidDate,
            @Param("status") PaymentStatus status
    );

    Optional<PaymentLog> findByOrderId(String orderId);
    boolean existsByOrderId(String orderId);

    Optional<PaymentLog> findByPaymentKey(String paymentKey);
    boolean existsByPaymentKey(String paymentKey);

    // 구독 결제로그 전체 조회(최신순)
    List<PaymentLog> findAllBySubscription_IdOrderByPaidAtDesc(Long subscriptionId);

    List<PaymentLog> findAllBySubscription_IdAndPaymentStatusOrderByPaidAtDesc(
            Long subscriptionId,
            PaymentStatus paymentStatus
    );

    Optional<PaymentLog> findTop1BySubscription_IdOrderByPaidAtDesc(Long subscriptionId);

    // ✅ subscriberId로 전체/최근 결제 조회
    List<PaymentLog> findAllBySubscription_Subscriber_IdOrderByPaidAtDesc(Long subscriberId);
    Optional<PaymentLog> findTop1BySubscription_Subscriber_IdOrderByPaidAtDesc(Long subscriberId);
}
