package org.example.homedatazip.payment.controller;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.config.CustomUserDetails;
import org.example.homedatazip.payment.dto.PaymentListResponse;
import org.example.homedatazip.payment.dto.PaymentLogResponse;
import org.example.homedatazip.payment.service.PaymentQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentQueryService paymentQueryService;

    /**
     * ✅ 내 결제 로그 전체 조회
     * GET /api/payments/me
     */
    @GetMapping("/me")
    public ResponseEntity<PaymentListResponse> myPayments(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();
        return ResponseEntity.ok(paymentQueryService.getMyPayments(userId));
    }

    /**
     * ✅ 내 최근 결제 1건 조회
     * GET /api/payments/me/latest
     */
    @GetMapping("/me/latest")
    public ResponseEntity<PaymentLogResponse> myLatestPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();
        return ResponseEntity.ok(paymentQueryService.getMyLatestPayment(userId));
    }
}
