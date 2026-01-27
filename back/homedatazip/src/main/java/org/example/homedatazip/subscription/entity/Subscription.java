package org.example.homedatazip.subscription.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

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

    @OneToOne
    private User subscriber;

    @Builder.Default
    private Long price = 9900L;

    @Builder.Default
    private String name = "기본 요금제";

    private SubscriptionStatus status;   // Enum 타입 Active, Canceled, Expired

    private String customerKey;   // 유저 쪽에서 생성, "cust_" + UUID.randomUUID()
    private String billingKey;

    private LocalDate startDate;
    private LocalDate endDate;

    @Builder.Default
    private boolean isActive ;   //만료시 false로

    //----메서드----
    public void expire() {
        this.status = SubscriptionStatus.EXPIRED;
        this.isActive = false;
    }

    public void cancelAutoPay() {
        this.status = SubscriptionStatus.CANCELED;
        this.isActive = true;
    }

    public void activate() {
        this.status = SubscriptionStatus.ACTIVE;
        this.isActive = true;
    }
}
