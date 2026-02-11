package org.example.homedatazip.monthAvg.dto;

import java.time.YearMonth;

public record MonthTop3JeonsePriceResponse(
        Long aptId,
        Double exclusive,
        String aptName,
        String gugun,
        String yyyymm,
        Integer jeonseCount,
        Long avgDeposit,
        Double depositChangeRate
){}
