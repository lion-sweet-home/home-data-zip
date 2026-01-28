package org.example.homedatazip.subscription.controller;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.config.CustomUserDetails;
import org.example.homedatazip.global.exception.BusinessException;
import org.example.homedatazip.global.exception.domain.PaymentErrorCode;
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
     * 1. 카드 등록 시작 (프론트에서 Toss Billing 인증 시작용)
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
     * 2. 카드 등록 성공(authKey) → billingKey 저장 (JWT 없음)
     * 토스에서 successUrl로 리다이렉트 들어오는 요청이라 Authorization 헤더가 없다.
     */
    @GetMapping("/billing/success")
    public ResponseEntity<Void> billingSuccess(
            @RequestParam String customerKey,
            @RequestParam String authKey
    ) {
        Long userId = parseUserIdFromCustomerKey(customerKey);

        subscriptionService.registerBillingKey(userId, customerKey, authKey);

        return ResponseEntity.status(302)
                .header("Location", "http://localhost:5173/billing/success")
                .build();
    }

    /**
     * 2-1. 카드 등록 실패 (JWT 없음)
     */
    @GetMapping("/billing/fail")
    public ResponseEntity<Void> billingFail(
            @RequestParam(required = false) String customerKey
    ) {
        return ResponseEntity.status(302)
                .header("Location", "http://localhost:5173/billing/fail")
                .build();
    }

    /**
     * 3. 첫 결제 = 구독 시작 (billingKey 결제 1회)
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

    // ===== private =====

    private Long parseUserIdFromCustomerKey(String customerKey) {
        if (customerKey == null || customerKey.isBlank() || !customerKey.startsWith("CUSTOMER_")) {
            throw new BusinessException(PaymentErrorCode.INVALID_CUSTOMER_KEY);
        }
        try {
            return Long.parseLong(customerKey.substring("CUSTOMER_".length()));
        } catch (NumberFormatException e) {
            throw new BusinessException(PaymentErrorCode.INVALID_CUSTOMER_KEY);
        }
    }
}
