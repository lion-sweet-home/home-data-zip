package org.example.homedatazip.tradeSale.dto;

import java.util.List;
import java.util.Map;

public record AptChartResponse(
        Map<Double, List<TradeSaleChartData>> pyeongChartDataMap
) {
}
