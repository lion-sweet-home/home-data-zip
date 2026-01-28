package org.example.homedatazip.payment.controller;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.config.CustomUserDetails;
import org.example.homedatazip.payment.dto.*;
import org.example.homedatazip.payment.service.PaymentQueryService;
import org.example.homedatazip.payment.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentQueryService paymentQueryService;

    @PostMapping("/prepare")
    public ResponseEntity<PaymentPrepareResponse> prepare(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody(required = false) PaymentPrepareRequest request
    ) {
        return ResponseEntity.ok(
                paymentService.prepareOneTimePayment(principal.getUserId(), request)
        );
    }

    @PostMapping("/confirm")
    public ResponseEntity<?> confirm(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody TossPaymentConfirmRequest request
    ) {
        // ✅ confirm이 컨트롤러까지 들어오는지 확인용
        return ResponseEntity.ok(request);
    }

    @GetMapping("/me")
    public ResponseEntity<PaymentListResponse> myPayments(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ResponseEntity.ok(
                paymentQueryService.getMyPayments(principal.getUserId())
        );
    }

    @GetMapping("/me/latest")
    public ResponseEntity<PaymentLogResponse> myLatestPayment(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ResponseEntity.ok(
                paymentQueryService.getMyLatestPayment(principal.getUserId())
        );
    }
}
