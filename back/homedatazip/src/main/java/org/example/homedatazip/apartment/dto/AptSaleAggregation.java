package org.example.homedatazip.apartment.dto;

public record AptSaleAggregation(
        Long aptId,
        Long areaTypeId,

        // 전월 집계
        Long lastMonthAmountSum,
        Long lastMonthSaleCount,

        // 비교 대상월(과거 중 거래가 있는 최신 월)
        String compareYyyymm,
        Long compareMonthAmountSum,
        Long compareMonthSaleCount
) {

    /**
     * 전월 평균 거래가
     */
    public Long getLastMonthAvgAmount() {
        if (lastMonthSaleCount == null || lastMonthSaleCount == 0) {
            return null;
        }

        return lastMonthAmountSum / lastMonthSaleCount;
    }

    /**
     * 비교 대상월 평균 거래가
     */
    public Long getCompareMonthAvgAmount() {
        if (compareMonthSaleCount == null || compareMonthSaleCount == 0) {
            return null;
        }

        return compareMonthAmountSum / compareMonthSaleCount;
    }

    /**
     * 등락률 계산
     * <br/>
     * (전월 평균 거래가 - 전전월 평균 거래가) / 전전월 평균 거래가 * 100
     */
    public Double getPriceChangeRate() {
        Long lastMonthAvg = getLastMonthAvgAmount();
        Long compareMonthAvg = getCompareMonthAvgAmount();

        if (lastMonthAvg == null || compareMonthAvg == null || compareMonthAvg == 0) {
            return null;
        }

        return ((lastMonthAvg.doubleValue() - compareMonthAvg.doubleValue())
                / compareMonthAvg.doubleValue()) * 100;
    }
}
