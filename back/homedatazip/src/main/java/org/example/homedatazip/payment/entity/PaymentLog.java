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

    //  PROCESSING 단계에서는 null 가능해야 함 (승인 성공 후 채움)
    @Column(name = "payment_key", unique = true, nullable = true, length = 200)
    private String paymentKey;

    @Column(name = "order_name", nullable = false, length = 100)
    private String orderName;

    @Lob
    @Column(name = "fail_reason", columnDefinition = "TEXT")
    private String failReason;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    public void markApproved(String paymentKey, String orderId, Long amount, LocalDateTime approvedAt) {
        this.paymentKey = paymentKey;
        this.orderId = orderId;
        this.amount = amount;
        this.paymentStatus = PaymentStatus.APPROVED;
        this.approvedAt = approvedAt;
        this.paidAt = approvedAt;
        this.failReason = null;
    }

    public void markFailed(String reason) {
        this.paymentStatus = PaymentStatus.FAILED;
        this.failReason = reason;
    }
}
