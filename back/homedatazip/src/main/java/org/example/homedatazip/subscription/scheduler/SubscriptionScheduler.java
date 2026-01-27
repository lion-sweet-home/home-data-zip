package org.example.homedatazip.subscription.scheduler;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.subscription.service.SubscriptionBatchService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class SubscriptionScheduler {

    private final SubscriptionBatchService batchService;

    // 매일 00:10 만료 처리
    @Scheduled(cron = "0 10 0 * * *")
    public void expireDaily() {
        batchService.expire(LocalDate.now());
    }

    // 매일 00:20 정기결제 처리
    @Scheduled(cron = "0 20 0 * * *")
    public void recurringPaymentDaily() {
        batchService.processRecurringPayment(LocalDate.now());
    }
}
