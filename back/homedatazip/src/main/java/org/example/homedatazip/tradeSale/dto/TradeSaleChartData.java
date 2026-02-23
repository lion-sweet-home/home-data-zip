package org.example.homedatazip.tradeSale.dto;

import java.util.List;

public record TradeSaleChartData(
        String month,
        Long avgAmount,
        Long tradeCount,
        List<IndividualTrade> dots
) {
}
