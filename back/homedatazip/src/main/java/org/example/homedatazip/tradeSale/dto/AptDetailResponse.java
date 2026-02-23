package org.example.homedatazip.tradeSale.dto;

import java.util.List;
import java.util.Map;

public record AptDetailResponse(
        String aptNm,
        String address,
        Long avgAmount,
        Map<Double, List<TradeSaleHistory>> pyeongTrades,
        Map<Double,List<TradeSaleChartData>> chartData,
        Integer buildYear
) {
}
