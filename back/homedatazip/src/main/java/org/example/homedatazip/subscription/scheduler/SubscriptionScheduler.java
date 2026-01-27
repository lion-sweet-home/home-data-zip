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

    private final SubscriptionBatchService subscriptionBatchService; // 만료만
    //private final PaymentBatchService paymentBatchService;           // 정기결제 실행만
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /**
     * ✅ 매일 00:10 (KST) : 만료 처리
     * - isActive=true
     * - status in (ACTIVE, CANCELED)
     * - endDate < today  => EXPIRED 처리
     */
    @Scheduled(cron = "0 10 0 * * *", zone = "Asia/Seoul")
    public void expireDaily() {
        LocalDate today = LocalDate.now(KST);
        try {
            int count = subscriptionBatchService.expire(today);
            log.info("[SubscriptionScheduler] expireDaily success. date={}, expiredCount={}", today, count);
        } catch (Exception e) {
            log.error("[SubscriptionScheduler] expireDaily failed. date={}", today, e);
        }
    }

//    /**
//     * ✅ 매일 00:20 (KST) : 정기결제 처리
//     * - 실제 결제/로그/구독연장은 payment 패키지에서 수행
//     *
//     * ⚠️ 중요: 여기서 돌리면 PaymentBatchService에는 @Scheduled 없어야 함.
//     */
//    @Scheduled(cron = "0 20 0 * * *", zone = "Asia/Seoul")
//    public void recurringPaymentDaily() {
//        LocalDate today = LocalDate.now(KST);
//        try {
//            var res = paymentBatchService.runRecurringPayment(today);
//            log.info("[SubscriptionScheduler] recurringPaymentDaily success. date={}, target={}, success={}, fail={}",
//                    today, res.targetCount(), res.successCount(), res.failCount());
//        } catch (Exception e) {
//            log.error("[SubscriptionScheduler] recurringPaymentDaily failed. date={}", today, e);
//        }
//    }
}
