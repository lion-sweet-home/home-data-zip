package org.example.homedatazip.subscription.controller;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.config.CustomUserDetails;
import org.example.homedatazip.subscription.dto.StartSubscriptionRequest;
import org.example.homedatazip.subscription.dto.SubscriptionStartResponse;
import org.example.homedatazip.subscription.service.SubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    // 구독 시작/재구독
    @PostMapping("/start")
    public ResponseEntity<SubscriptionStartResponse> startSubscription(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody StartSubscriptionRequest request
    ) {
        Long subscriberId = principal.getUserId();

        SubscriptionStartResponse response = subscriptionService.startSubscription(
                subscriberId,
                request.name(),
                request.price(),
                request.periodDays()
        );

        return ResponseEntity.ok(response);
    }

    // 자동결제 OFF (다음 결제부터 중단)
    @PostMapping("/auto-pay/cancel")
    public ResponseEntity<Void> cancelAutoPay(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        subscriptionService.cancelAutoPay(principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    // 자동결제 ON (다시 켜기)
    @PostMapping("/auto-pay/reactivate")
    public ResponseEntity<Void> reactivateAutoPay(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        subscriptionService.reactivateAutoPay(principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    // 내 구독 조회
    @GetMapping("/me")
    public ResponseEntity<SubscriptionStartResponse> getMySubscription(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ResponseEntity.ok(subscriptionService.getMySubscription(principal.getUserId()));
    }
}
