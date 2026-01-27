package org.example.homedatazip.subscription.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.subscription.service.SubscriptionBatchService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionScheduler {

    private final SubscriptionBatchService batchService;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    // 매일 00:10 만료 처리 (KST 기준)
    @Scheduled(cron = "0 10 0 * * *", zone = "Asia/Seoul")
    public void expireDaily() {
        LocalDate today = LocalDate.now(KST);
        try {
            int count = batchService.expire(today);
            log.info("[SubscriptionScheduler] expireDaily success. date={}, expiredCount={}", today, count);
        } catch (Exception e) {
            log.error("[SubscriptionScheduler] expireDaily failed. date={}", today, e);
        }
    }

    // 매일 00:20 정기결제 처리 (KST 기준)
    @Scheduled(cron = "0 20 0 * * *", zone = "Asia/Seoul")
    public void recurringPaymentDaily() {
        LocalDate today = LocalDate.now(KST);
        try {
            int successCount = batchService.processRecurringPayment(today);
            log.info("[SubscriptionScheduler] recurringPaymentDaily success. date={}, successCount={}", today, successCount);
        } catch (Exception e) {
            log.error("[SubscriptionScheduler] recurringPaymentDaily failed. date={}", today, e);
        }
    }
}
