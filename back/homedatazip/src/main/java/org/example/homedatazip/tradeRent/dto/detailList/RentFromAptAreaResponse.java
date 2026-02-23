package org.example.homedatazip.tradeRent.dto.detailList;

import org.example.homedatazip.tradeRent.entity.TradeRent;

import java.time.LocalDate;

public record RentFromAptAreaResponse(
        Long deposit,
        Integer monthlyRent,
        LocalDate dealDate,
        Integer floor
) {
    public static RentFromAptAreaResponse from(TradeRent tradeRent) {
        return new RentFromAptAreaResponse(
                tradeRent.getDeposit(),
                tradeRent.getMonthlyRent(),
                tradeRent.getDealDate(),
                tradeRent.getFloor()
        );
    }
}
