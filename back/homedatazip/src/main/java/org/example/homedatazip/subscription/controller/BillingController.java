package org.example.homedatazip.subscription.controller;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.config.CustomUserDetails;
import org.example.homedatazip.subscription.dto.BillingAuthFailRequest;
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

    /**
     * 카드 등록 시작(프론트가 Toss SDK requestBillingAuth 호출에 필요한 값 받기)
     * POST /api/subscriptions/billing/issue
     */
    @PostMapping("/issue")
    public ResponseEntity<BillingKeyIssueResponse> issue(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody BillingKeyIssueRequest request
    ) {
        return ResponseEntity.ok(billingService.issue(principal.getUserId(), request));
    }

    /**
     * 카드 등록 성공 콜백(프론트가 success 페이지에서 파라미터 잡아서 서버로 전달)
     * POST /api/subscriptions/billing/success
     */
    @PostMapping("/success")
    public ResponseEntity<Void> success(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody BillingAuthSuccessRequest request
    ) {
        billingService.onAuthSuccess(principal.getUserId(), request);
        return ResponseEntity.noContent().build();
    }

    /**
     * 카드 등록 실패 콜백
     * POST /api/subscriptions/billing/fail
     */
    @PostMapping("/fail")
    public ResponseEntity<Void> fail(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody BillingAuthFailRequest request
    ) {
        billingService.onAuthFail(principal.getUserId(), request);
        return ResponseEntity.noContent().build();
    }

    /**
     * (옵션) 카드 삭제/초기화
     * DELETE /api/subscriptions/billing
     */
    @DeleteMapping
    public ResponseEntity<Void> clear(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        billingService.clearBillingKey(principal.getUserId());
        return ResponseEntity.noContent().build();
    }
}
