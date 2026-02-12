package org.example.homedatazip.monthAvg.dto;

public record MonthTop3SalePriceResponse(
        Long aptId,
        Double exclusive,
        String aptName,
        String gugun,
        String dong,
        String yyyymm,
        Integer saleCount,
        Long avgDealAmount,
        Double dealAmountChangeRate
) {}

