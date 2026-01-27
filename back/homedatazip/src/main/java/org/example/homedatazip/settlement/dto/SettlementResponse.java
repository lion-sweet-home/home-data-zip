package org.example.homedatazip.settlement.dto;

import org.example.homedatazip.settlement.entity.Settlement;

import java.time.LocalDate;

public record SettlementResponse(
        int year,
        int month,
        Long amount,
        LocalDate periodStart,
        LocalDate periodEnd
) {

    public static SettlementResponse from(Settlement settlement) {
        return new SettlementResponse(
                settlement.getPeriodStart().getYear(),
                settlement.getPeriodStart().getMonthValue(),
                settlement.getAmount(),
                settlement.getPeriodStart(),
                settlement.getPeriodEnd()
        );
    }
}
