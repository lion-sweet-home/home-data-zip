package org.example.homedatazip.subscription.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.homedatazip.subscription.type.SubscriptionStatus;
import org.example.homedatazip.user.entity.User;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscriber_id", nullable = false, unique = true)
    private User subscriber;

    @Builder.Default
    @Column(nullable = false)
    private Long price = 9900L;

    @Builder.Default
    @Column(nullable = false, length = 50)
    private String name = "기본 요금제";

    /**
     * - ACTIVE   : 권한 있음 + 자동결제 ON
     * - CANCELED : 권한 있음 + 자동결제 OFF (기간은 endDate까지 유지)
     * - EXPIRED  : 권한 없음
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionStatus status;

    @Column(length = 100)
    private String customerKey;

    @Column(length = 200)
    private String billingKey;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private boolean isActive;

    // ---- 도메인 메서드 ----

    public boolean hasAccess(LocalDate today) {
        return isActive && endDate != null && !endDate.isBefore(today)
                && (status == SubscriptionStatus.ACTIVE || status == SubscriptionStatus.CANCELED);
    }

    public void expire() {
        this.status = SubscriptionStatus.EXPIRED;
        this.isActive = false;
    }

    /** 자동결제 OFF (권한은 유지) */
    public void cancelAutoPay() {
        this.status = SubscriptionStatus.CANCELED;
        this.isActive = true;
    }

    /** 자동결제 ON */
    public void activateAutoPay() {
        this.status = SubscriptionStatus.ACTIVE;
        this.isActive = true;
    }

    public void extendOneMonth() {
        this.endDate = this.endDate.plusMonths(1);
    }

    public void updatePlan(String name, Long price) {
        this.name = name;
        this.price = price;
    }

    public void resetPeriod(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }
}
