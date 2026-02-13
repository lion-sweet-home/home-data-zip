package org.example.homedatazip.monthAvg.dto;

public record MonthTop3WolsePriceResponse(
        Long aptId,
        Double exclusive,
        String aptName,
        String gugun,
        String dong,
        String yyyymm,
        Integer wolseCount,
        Long avgDeposit,
        Double depositChangeRate,
        Long avgMonthRent,
        Double monthRentChangeRate
) {}
