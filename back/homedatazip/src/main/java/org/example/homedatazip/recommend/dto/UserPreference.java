package org.example.homedatazip.recommend.dto;

public record UserPreference(
        String favoriteSggCode,
        Long preferredPrice,
        Long preferredMonthly,
        Double preferredArea,
        boolean isRent,
        boolean isWolse
) {

    public UserPreference(
            String favoriteSggCode,
            Long priceSum, Long monthlySum, Number areaSum,  // AreaSum을 Number로 받음
            Long priceCnt, Long monthlyCnt, Long areaCnt,
            Long rentScore, Long saleScore, Long wolseScore, Long jeonseScore
    ) {
        this(
                favoriteSggCode,
                (priceCnt != null && priceCnt > 0) ? priceSum / priceCnt : 0L,
                (monthlyCnt != null && monthlyCnt > 0) ? monthlySum / monthlyCnt : 0L,
                (areaCnt != null && areaCnt > 0) ? areaSum.doubleValue() / areaCnt : 0.0,
                (rentScore != null && saleScore != null) && (rentScore > saleScore),
                (wolseScore != null && jeonseScore != null) && (wolseScore > jeonseScore)
        );
    }

}
