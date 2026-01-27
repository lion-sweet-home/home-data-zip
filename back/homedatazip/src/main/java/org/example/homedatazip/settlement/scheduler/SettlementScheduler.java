package org.example.homedatazip.settlement.scheduler;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.payment.entity.PaymentStatus;
import org.example.homedatazip.payment.repository.PaymentLogRepository;
import org.example.homedatazip.settlement.entity.Settlement;
import org.example.homedatazip.settlement.repository.SettlementRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class SettlementScheduler {

    private final SettlementRepository settlementRepository;
    private final PaymentLogRepository paymentLogRepository;

    /**
     * 매일 00:30분
     * 전날 금액을 해당 월 Settlement에 누적
     */
    @Scheduled(cron = "0 30 0 * * *")
    @Transactional
    public void updateDailySettlement() {

        LocalDate yesterday = LocalDate.now().minusDays(1);

        // 전날 결제 금액 합계 조회
        Long amountPaidYesterday
                = paymentLogRepository.sumAmountByPaidDate(
                        yesterday,
                        PaymentStatus.APPROVED
        );

        // 결제 내역 없으면 종료
        if (amountPaidYesterday == null || amountPaidYesterday == 0L) {
            return;
        }

        // 해당 월의 Settlement 조회 또는 새로운 월이라면 Settlement 생성
        Settlement settlement
                = settlementRepository.findByPeriodStartAndPeriodEnd(
                        yesterday.withDayOfMonth(1),
                        yesterday.withDayOfMonth(yesterday.lengthOfMonth())
                )
                .orElseGet(() -> Settlement.createMonthly(yesterday));

        // 금액 누적 및 저장
        settlement.addAmount(amountPaidYesterday);
        settlementRepository.save(settlement);
    }
}
