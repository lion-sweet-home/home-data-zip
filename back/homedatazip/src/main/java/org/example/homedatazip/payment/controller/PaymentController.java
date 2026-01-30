package org.example.homedatazip.payment.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.global.config.CustomUserDetails;
import org.example.homedatazip.payment.dto.*;
import org.example.homedatazip.payment.service.PaymentQueryService;
import org.example.homedatazip.payment.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
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
        log.info("[PAYMENT] prepare userId={}", principal.getUserId());
        return ResponseEntity.ok(
                paymentService.prepareOneTimePayment(principal.getUserId(), request)
        );
    }

    @PostMapping("/confirm")
    public ResponseEntity<PaymentConfirmResponse> confirm(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody TossPaymentConfirmRequest request
    ) {
        log.info("[PAYMENT] confirm request userId={}, orderId={}, requestAmount={}",
                principal.getUserId(),
                request.orderId(),
                request.amount()
        );

        return ResponseEntity.ok(
                paymentService.confirmOneTimePayment(principal.getUserId(), request)
        );
    }

    @GetMapping("/me")
    public ResponseEntity<PaymentListResponse> myPayments(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        log.info("[PAYMENT] myPayments userId={}", principal.getUserId());
        return ResponseEntity.ok(
                paymentQueryService.getMyPayments(principal.getUserId())
        );
    }

    @GetMapping("/me/latest")
    public ResponseEntity<PaymentLogResponse> myLatestPayment(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        log.info("[PAYMENT] myLatestPayment userId={}", principal.getUserId());
        return ResponseEntity.ok(
                paymentQueryService.getMyLatestPayment(principal.getUserId())
        );
    }
}
