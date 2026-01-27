package org.example.homedatazip.subscription.controller;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.subscription.service.SubscriptionBatchService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/subscriptions/batch")
public class SubscriptionAdminBatchController {

    private final SubscriptionBatchService subscriptionBatchService;

    /**
     * 만료 배치 수동 실행
     * POST /api/admin/subscriptions/batch/expire?date=2026-01-27
     */
    @PostMapping("/expire")
    public ResponseEntity<ExpireBatchResponse> expire(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        int expiredCount = subscriptionBatchService.expire(date);
        return ResponseEntity.ok(new ExpireBatchResponse(expiredCount));
    }

    /**
     * 정기결제 배치 수동 실행
     * POST /api/admin/subscriptions/batch/recurring?date=2026-01-27
     */
    @PostMapping("/recurring")
    public ResponseEntity<RecurringBatchResponse> recurring(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        int successCount = subscriptionBatchService.processRecurringPayment(date);
        return ResponseEntity.ok(new RecurringBatchResponse(successCount));
    }

    public record ExpireBatchResponse(int expiredCount) {}
    public record RecurringBatchResponse(int successCount) {}
}
