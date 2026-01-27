package org.example.homedatazip.subscription.controller;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.config.CustomUserDetails;
import org.example.homedatazip.subscription.dto.BillingAuthSuccessRequest;
import org.example.homedatazip.subscription.dto.BillingKeyIssueRequest;
import org.example.homedatazip.subscription.dto.BillingKeyIssueResponse;
import org.example.homedatazip.subscription.service.BillingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/subscriptions/billing")
public class BillingController {

    private final BillingService billingService;

    // 카드 등록 시작용 정보 발급
    @PostMapping("/issue")
    public ResponseEntity<BillingKeyIssueResponse> issue(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody BillingKeyIssueRequest request
    ) {
        return ResponseEntity.ok(billingService.issue(principal.getUserId(), request));
    }

    // 카드 등록 성공 -> billingKey 저장
    @PostMapping("/success")
    public ResponseEntity<Void> success(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody BillingAuthSuccessRequest request
    ) {
        billingService.onAuthSuccess(principal.getUserId(), request);
        return ResponseEntity.noContent().build();
    }
}
