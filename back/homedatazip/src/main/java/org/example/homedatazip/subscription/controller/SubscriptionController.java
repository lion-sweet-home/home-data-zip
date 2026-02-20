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

    @PostMapping("/billing/issue")
    public ResponseEntity<BillingKeyIssueResponse> issueBilling(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody(required = false) BillingKeyIssueRequest request
    ) {
        return ResponseEntity.ok(
                subscriptionService.issueBillingKey(principal.getUserId(), request)
        );
    }


    @GetMapping("/billing/success")
    public ResponseEntity<Void> billingSuccess(
            @RequestParam String customerKey,
            @RequestParam String authKey
    ) {
        Long userId = parseUserIdFromCustomerKey(customerKey);

        subscriptionService.registerBillingKey(userId, customerKey, authKey);

        return ResponseEntity.status(302)
//                .header("Location", "http://localhost:5173/billing/success")
                .header("Location", "http://localhost:3000/subscription/success")
                .build();
    }


    @GetMapping("/billing/fail")
    public ResponseEntity<Void> billingFail(
            @RequestParam(required = false) String customerKey
    ) {
        return ResponseEntity.status(302)
//                .header("Location", "http://localhost:5173/billing/fail")
                .header("Location", "http://localhost:3000/subscription/fail")
                .build();
    }


    @PostMapping("/start")
    public ResponseEntity<Void> startSubscription(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        subscriptionService.startSubscription(principal.getUserId());
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/auto-pay/cancel")
    public ResponseEntity<Void> cancelAutoPay(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        subscriptionService.cancelAutoPay(principal.getUserId());
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/auto-pay/reactivate")
    public ResponseEntity<Void> reactivateAutoPay(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        subscriptionService.reactivateAutoPay(principal.getUserId());
        return ResponseEntity.noContent().build();
    }


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
