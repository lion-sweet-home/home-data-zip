package org.example.homedatazip.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.homedatazip.common.BaseTimeEntity;
import org.example.homedatazip.payment.type.PaymentStatus;
import org.example.homedatazip.subscription.entity.Subscription;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(
        name = "payment_log",
        indexes = {
                @Index(name = "idx_payment_subscription", columnList = "subscription_id"),
                @Index(name = "idx_payment_status", columnList = "payment_status")
        }
)
public class PaymentLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(name ="order_id", nullable = false, unique = true, length = 64)
    private String orderId;

    // 토스 결제건 식별자 (승인/취소/조회에 필요)
    @Column(name = "payment_key", unique = true, length = 200)
    private String paymentKey;

    @Column(name = "orderName", nullable = false, length = 100)
    private String orderName;

    @Column(name = "fail_reason")
    private String failReason;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus;

    // 토스 승인 시간
    private LocalDateTime approvedAt;

    // 결제 완료 처리 시간
    private LocalDateTime paidAt;

    // ----- 메서드 -----

    // 토스 결제 승인 성공시
    public void markApproved(String paymentKey, LocalDateTime approvedAt, LocalDateTime paidAt) {
        this.paymentStatus = PaymentStatus.APPROVED;
        this.paymentKey = paymentKey;
        this.approvedAt = approvedAt;
        this.paidAt = paidAt;
        this.failReason = null;
    }

    // 승인 실패
    public void markFailed(String failReason) {
        this.paymentStatus = PaymentStatus.FAILED;
        this.failReason = failReason;
    }

    public void markCanceled() {
        this.paymentStatus = PaymentStatus.CANCELED;
    }
}
