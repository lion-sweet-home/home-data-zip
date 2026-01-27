package org.example.homedatazip.payment.controller;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.payment.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    // customerKey 조회
    @GetMapping("/customer-key")
    public ResponseEntity<CustomerKeyResponse> getCustomerKey(
            @RequestParam Long userId
    ) {
        CustomerKeyResponse response = paymentService.getCustomerKey(userId);
        return ResponseEntity.ok(response);
    }

    // 카드 등록 → billingKey 발급
    @PostMapping("/billing-keys")
    public ResponseEntity<BillingKeyResponse> issueBillingKey(
            @RequestBody IssueBillingKeyRequest request
    ) {
        BillingKeyResponse response = paymentService.issueBillingKey(
                request.userId(),
                request.authKey()
        );
        return ResponseEntity.ok(response);
    }

    // 내 결제 내역 조회
    @GetMapping("/me")
    public ResponseEntity<?> getMyPayments(
            @RequestParam Long userId
    ) {
        return ResponseEntity.ok(paymentService.getMyPayments(userId));
    }
}