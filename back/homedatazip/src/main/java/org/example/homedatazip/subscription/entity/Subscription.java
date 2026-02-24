package org.example.homedatazip.subscription.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.homedatazip.subscription.type.SubscriptionStatus;
import org.example.homedatazip.user.entity.User;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
    @OnDelete(action = OnDeleteAction.CASCADE)
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

    @Column(name = "billing_key", length = 200)
    private String billingKey;

    @Column(name = "billing_key_issued_at")
    private LocalDateTime billingKeyIssuedAt;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private boolean isActive;

    // ---- 도메인 메서드 ----

    public boolean hasAccess(LocalDate today) {
        return isActive
                && endDate != null
                && !endDate.isBefore(today)
                && (status == SubscriptionStatus.ACTIVE || status == SubscriptionStatus.CANCELED);
    }

    public void expire() {
        this.status = SubscriptionStatus.EXPIRED;
        this.isActive = false;
    }

    // 자동결제 OFF (권한은 유지)
    public void cancelAutoPay() {
        this.status = SubscriptionStatus.CANCELED;
        this.isActive = true;
    }

    // 자동결제 ON
    public void activateAutoPay() {
        this.status = SubscriptionStatus.ACTIVE;
        this.isActive = true;
    }

    /**
     * 배치 결제 성공 시 기간 갱신
     * - 기본: endDate + 1개월
     * - 안전: endDate가 null이면 startDate 기준으로 잡아줌
     */
    public void extendOneMonth() {
        if (this.endDate == null) {
            // 방어코드: endDate가 비어있으면 startDate 기반으로라도 세팅
            LocalDate base = (this.startDate != null) ? this.startDate : LocalDate.now();
            this.endDate = base.plusMonths(1);
            return;
        }
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

    // 카드 등록 성공(빌링키 발급) 시 저장
    public void registerBillingKey(String billingKey) {
        this.billingKey = billingKey;
        this.billingKeyIssuedAt = LocalDateTime.now();
    }

    // 카드 등록 해지/초기화가 필요하면 사용
    public void clearBillingKey() {
        this.billingKey = null;
        this.billingKeyIssuedAt = null;
    }

    // 결제 승인 성공(첫 결제) -> ACTIVE로 전환 + 기간 세팅
    public void activateAfterFirstPayment(LocalDate startDate, LocalDate endDate) {
        this.status = SubscriptionStatus.ACTIVE;
        this.isActive = true;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public static Subscription createInitial(User user) {
        LocalDate today = LocalDate.now();

        return Subscription.builder()
                .subscriber(user)
                .name("미구독")
                .price(0L)
                .status(SubscriptionStatus.NONE) // 미구독 상태
                .isActive(false)                 // 권한 없음
                .startDate(today)                // not null 제약 때문에 임시값
                .endDate(today)                  // not null 제약 때문에 임시값
                .build();
    }

    public void start(LocalDate today, Long price) {
        this.status = SubscriptionStatus.ACTIVE;
        this.isActive = true;
        this.price = price;
        this.startDate = today;
        this.endDate = today.plusMonths(1);
    }

    public boolean hasBillingKey() {
        return this.billingKey != null && !this.billingKey.isBlank();
    }

    public void updateBillingKey(String billingKey) {
        this.billingKey = billingKey;
    }
}
