package org.example.homedatazip.settlement.scheduler;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.payment.type.PaymentStatus;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.payment.repository.PaymentLogRepository;
import org.example.homedatazip.settlement.entity.Settlement;
import org.example.homedatazip.settlement.repository.SettlementRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementScheduler {

    private final SettlementRepository settlementRepository;
    private final PaymentLogRepository paymentLogRepository;

    /**
     * 매일 00:30분
     * 전날 금액을 해당 월 Settlement에 누적
     */
//    @Scheduled(cron = "0 * * * * *") // 테스트용
    @Scheduled(cron = "0 30 0 * * *")
    @Transactional
    public void updateDailySettlement() {

        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("🔄 스케줄러 실행 - 대상 날짜: {}", yesterday);

        // 1. 전날 결제 금액 합계 조회
        Long amountPaidYesterday
                = paymentLogRepository.sumAmountByPaidDate(
                        yesterday,
                        PaymentStatus.APPROVED
        );

        log.info("{} 결제 금액: {}", yesterday, amountPaidYesterday);

        // 2. 결제 내역 없으면 종료
        if (amountPaidYesterday == null || amountPaidYesterday == 0L) {
            log.info("결제 내역 없음");
            return;
        }

        // 3. 해당 월의 Settlement 조회 또는 새로운 월이라면 Settlement 생성
        Settlement settlement
                = settlementRepository.findByPeriodStartAndPeriodEnd(
                        yesterday.withDayOfMonth(1),
                        yesterday.withDayOfMonth(yesterday.lengthOfMonth())
                )
                .orElseGet(() -> {
                            log.info("새로운 Settlement 생성: {}년 {}월",
                                    yesterday.getYear(),
                                    yesterday.getMonthValue()
                            );
                            return Settlement.createMonthly(yesterday);
                        }
                );

        // 4. 금액 누적 및 저장
        Long beforeAmount = settlement.getAmount();
        settlement.addAmount(amountPaidYesterday);
        settlementRepository.save(settlement);

        log.info("Settlement 업데이트 완료 - {}년 {}월: {} -> {}",
                yesterday.getYear(),
                yesterday.getMonthValue(),
                beforeAmount,
                settlement.getAmount()
        );
    }

    /**
     * 수동 정산 (테스트용)
     */
    @Transactional
    public void processSettlement(LocalDate targetDate) {
        log.info("📊 정산 처리 시작 - 대상 날짜: {}", targetDate);

        // 1. 해당 날짜 결제 금액 합계 조회
        Long amount = paymentLogRepository.sumAmountByPaidDate(
                targetDate,
                PaymentStatus.APPROVED
        );

        log.info("💰 {} 결제 금액: {}", targetDate, amount);

        // 2. 결제 내역 없으면 종료
        if (amount == null || amount == 0L) {
            log.info("⏭️ 결제 내역 없음. 스킵.");
            return;
        }

        // 3. 해당 월의 Settlement 조회 또는 생성
        Settlement settlement = settlementRepository
                .findByPeriodStartAndPeriodEnd(
                        targetDate.withDayOfMonth(1),
                        targetDate.withDayOfMonth(targetDate.lengthOfMonth())
                )
                .orElseGet(() -> {
                        log.info("🆕 새로운 Settlement 생성: {}년 {}월",
                                targetDate.getYear(),
                                targetDate.getMonthValue()
                        );
                        return Settlement.createMonthly(targetDate);
                    }
                );

        // 4. 금액 누적 및 저장
        Long beforeAmount = settlement.getAmount();
        settlement.addAmount(amount);
        settlementRepository.save(settlement);

        log.info("✅ Settlement 업데이트 완료 - {}년 {}월: {} → {}",
                targetDate.getYear(),
                targetDate.getMonthValue(),
                beforeAmount,
                settlement.getAmount()
        );
    }
}
