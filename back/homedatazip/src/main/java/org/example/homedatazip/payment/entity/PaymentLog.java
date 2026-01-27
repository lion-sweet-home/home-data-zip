package org.example.homedatazip.payment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.example.homedatazip.common.BaseTimeEntity;
import org.example.homedatazip.subscription.entity.Subscription;

import java.time.LocalDateTime;

@Entity
@Getter
public class PaymentLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;

    @Column(name ="order_id", nullable = false, unique = true)
    private String orderId;

    @Column(name = "orderName", nullable = false)
    private String orderName;

    @Column(name = "fail_reason")
    private String failReason;

    private Long amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    private LocalDateTime approvedAt;

    private LocalDateTime paidAt;
}
