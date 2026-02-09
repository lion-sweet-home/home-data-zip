package org.example.homedatazip.tradeSale.dto;

public record TradeSaleChartData(
        String month,
        Long avgAmount,
        Long tradeCount
) {
}
