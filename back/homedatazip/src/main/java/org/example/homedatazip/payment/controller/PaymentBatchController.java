package org.example.homedatazip.payment.controller;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.payment.dto.BillingRecurringResultResponse;
import org.example.homedatazip.payment.service.PaymentBatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/payments/batch")
public class PaymentBatchController {

    private final PaymentBatchService paymentBatchService;

    /**
     * 테스트용: 정기결제 배치 수동 실행
     * POST /api/admin/payments/batch/recurring?date=2026-01-29
     */
    @PostMapping("/recurring")
    public ResponseEntity<BillingRecurringResultResponse> runRecurring(
            @RequestParam(required = false) LocalDate date
    ) {
        LocalDate target = (date != null) ? date : LocalDate.now();
        return ResponseEntity.ok(paymentBatchService.runRecurringPayment(target));
    }
}
