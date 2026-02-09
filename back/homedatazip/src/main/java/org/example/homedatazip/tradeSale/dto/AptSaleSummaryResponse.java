package org.example.homedatazip.tradeSale.dto;

import java.util.List;

// 아파트 요약용
public record AptSaleSummaryResponse(
        String aptNm,
        List<Long> monthlyVolumes,
        List<RecentTradeSale> recentTradeSales,
        List<String> monthLabels
) {
}
