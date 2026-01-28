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
public class SubscriptionBatchController {

    private final SubscriptionBatchService subscriptionBatchService;

    @PostMapping("/expire")
    public ResponseEntity<ExpireBatchResponse> expire(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        int expiredCount = subscriptionBatchService.expire(date);
        return ResponseEntity.ok(new ExpireBatchResponse(expiredCount));
    }

    public record ExpireBatchResponse(int expiredCount) {}
}
