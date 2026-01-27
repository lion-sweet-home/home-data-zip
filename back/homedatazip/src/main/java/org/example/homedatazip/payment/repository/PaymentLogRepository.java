package org.example.homedatazip.payment.repository;

import org.example.homedatazip.payment.entity.PaymentLog;
import org.example.homedatazip.payment.type.PaymentStatus;
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

    Optional<PaymentLog> findByPaymentKey(String paymentKey);

    boolean existsByOrderId(String orderId);

    boolean existsByPaymentKey(String paymentKey);

    List<PaymentLog> findAllBySubscription_IdOrderByCreatedAtDesc(Long subscriptionId);

    List<PaymentLog> findAllByPaymentStatusOrderByCreatedAtDesc(PaymentStatus paymentStatus);
}
