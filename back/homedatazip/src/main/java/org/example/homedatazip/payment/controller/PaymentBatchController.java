package org.example.homedatazip.payment.controller;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.payment.dto.BillingRecurringResultResponse;
import org.example.homedatazip.payment.service.PaymentBatchService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/payments/batch")
public class PaymentBatchController {

    private final PaymentBatchService paymentBatchService;

    /**
     * ✅ 정기결제 배치 수동 실행 (테스트용)
     * POST /api/admin/payments/batch/recurring?date=2026-01-27
     */
    @PostMapping("/recurring")
    public ResponseEntity<BillingRecurringResultResponse> recurring(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(paymentBatchService.runRecurringPayment(date));
    }
}
