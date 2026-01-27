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

    // orderId로 단건 조회(중복 방지/확인용)
    Optional<PaymentLog> findByOrderId(String orderId);

    boolean existsByOrderId(String orderId);

    // 토스 paymentKey로 조회(승인/취소/조회 대응용)
    Optional<PaymentLog> findByPaymentKey(String paymentKey);

    boolean existsByPaymentKey(String paymentKey);

    // 내 구독 결제로그 전체 조회(최신순)
    List<PaymentLog> findAllBySubscription_IdOrderByPaidAtDesc(Long subscriptionId);

    // 특정 상태만 조회(예: FAILED만)
    List<PaymentLog> findAllBySubscription_IdAndPaymentStatusOrderByPaidAtDesc(
            Long subscriptionId,
            PaymentStatus paymentStatus
    );

    // 구독의 최근 결제 1건(중복 결제 방지/화면용)
    Optional<PaymentLog> findTop1BySubscription_IdOrderByPaidAtDesc(Long subscriptionId);
}
