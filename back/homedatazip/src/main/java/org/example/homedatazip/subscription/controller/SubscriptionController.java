package org.example.homedatazip.subscription.controller;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.config.CustomUserDetails;
import org.example.homedatazip.subscription.dto.*;
import org.example.homedatazip.subscription.service.SubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    /**
     * 1.카드 등록 시작 (프론트에서 Toss Billing 인증 시작용)
     */
    @PostMapping("/billing/issue")
    public ResponseEntity<BillingKeyIssueResponse> issueBilling(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody(required = false) BillingKeyIssueRequest request
    ) {
        return ResponseEntity.ok(
                subscriptionService.issueBillingKey(principal.getUserId(), request)
        );
    }

    /**
     * 2️. 카드 등록 성공(authKey) → billingKey 저장
     */
    @PostMapping("/billing/success")
    public ResponseEntity<Void> billingSuccess(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false) String authKey,
            @RequestParam(required = false) String customerKey,
            @RequestBody(required = false) BillingAuthSuccessRequest body
    ) {
        String finalAuthKey = (authKey != null && !authKey.isBlank())
                ? authKey
                : (body != null ? body.authKey() : null);

        String finalCustomerKey = (customerKey != null && !customerKey.isBlank())
                ? customerKey
                : (body != null ? body.customerKey() : null);

        subscriptionService.successBillingAuth(
                principal.getUserId(),
                new BillingAuthSuccessRequest(finalCustomerKey, finalAuthKey)
        );

        return ResponseEntity.noContent().build();
    }



    /**
     * 3️. 첫 결제 = 구독 시작 (billingKey 결제 1회)
     */
    @PostMapping("/start")
    public ResponseEntity<Void> startSubscription(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        subscriptionService.startSubscription(principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    /**
     * 자동결제 OFF (다음 결제부터 중단)
     */
    @PostMapping("/auto-pay/cancel")
    public ResponseEntity<Void> cancelAutoPay(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        subscriptionService.cancelAutoPay(principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    /**
     * 자동결제 ON
     */
    @PostMapping("/auto-pay/reactivate")
    public ResponseEntity<Void> reactivateAutoPay(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        subscriptionService.reactivateAutoPay(principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/billing/fail")
    public ResponseEntity<Void> billingFail(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody(required = false) Object body
    ) {
        return ResponseEntity.noContent().build();
    }

    /**
     * 내 구독 조회
     */
    @GetMapping("/me")
    public ResponseEntity<SubscriptionMeResponse> getMySubscription(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ResponseEntity.ok(
                subscriptionService.getMySubscription(principal.getUserId())
        );
    }
}
