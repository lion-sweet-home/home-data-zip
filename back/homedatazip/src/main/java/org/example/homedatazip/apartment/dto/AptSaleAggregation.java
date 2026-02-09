package org.example.homedatazip.apartment.dto;

public record AptSaleAggregation(
        Long aptId,

        // 6개월 집계
        Long sixMonthAmountSum,
        Long sixMonthSaleCount,

        // 전전월 집계
        Long twoMonthsAgoAmountSum,
        Long twoMonthsAgoSaleCount,

        // 전월 집계
        Long lastMonthAmountSum,
        Long lastMonthSaleCount
) {

    /**
     * 6개월 평균 거래가
     */
    public Long getSixMonthAvgAmount() {
        if (sixMonthSaleCount == null || sixMonthSaleCount == 0) {
            return null;
        }

        return sixMonthAmountSum / sixMonthSaleCount;
    }

    /**
     * 전전월 평균 거래가
     */
    public Long getTwoMonthsAgoAvgAmount() {
        if (twoMonthsAgoSaleCount == null || twoMonthsAgoSaleCount == 0) {
            return null;
        }

        return twoMonthsAgoAmountSum / twoMonthsAgoSaleCount;
    }

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
     * 등락률 계산
     * <br/>
     * (전월 평균 거래가 - 전전월 평균 거래가) / 전전월 평균 거래가 * 100
     */
    public Double getPriceChangeRate() {
        Long lastMonthAvg = getLastMonthAvgAmount();
        Long twoMonthsAgoAvg = getTwoMonthsAgoAvgAmount();

        if (lastMonthAvg == null || twoMonthsAgoAvg == null || twoMonthsAgoAvg == 0) {
            return null;
        }

        return ((lastMonthAvg.doubleValue() - twoMonthsAgoAvg.doubleValue())
                / twoMonthsAgoAvg.doubleValue()) * 100;
    }
}
