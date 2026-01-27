package org.example.homedatazip.subscription.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import org.example.homedatazip.user.entity.User;

import java.time.LocalDate;

@Entity
@Getter
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    private User subscriber;

//    @Builder.Default
//    private Long price = 9900;

    private String name = "기본 요금제";

    private SubscriptionStatus status;   // Enum 타입 Active, Canceled, Pause, Expired

    private String customerKey;   // 유저 쪽에서 생성
    private String billingKey;

    private LocalDate startDate;
    private LocalDate endDate;

    private boolean isActive;
}

