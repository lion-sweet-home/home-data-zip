package org.example.homedatazip.monthAvg.utill;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public final class Yyyymm {

    private static final DateTimeFormatter YYYYMM = DateTimeFormatter.ofPattern("yyyyMM");

    private Yyyymm() {}

    /** 지난달 yyyymm (예: 오늘이 2026-02-03이면 202601) */
    public static String lastMonthYyyymm(LocalDate today) {
        return YearMonth.from(today).minusMonths(1).format(YYYYMM);
    }

    /** (maxYyyymm 기준) n개월 범위의 minYyyymm 계산: 예) n=6 -> 6개월치 */
    public static String minYyyymmForMonths(String maxYyyymm, int months) {
        if (months <= 0) throw new IllegalArgumentException("months must be positive");
        YearMonth max = YearMonth.parse(maxYyyymm, YYYYMM);
        YearMonth min = max.minusMonths(months - 1L);
        return min.format(YYYYMM);
    }

    /** (maxYyyymm 기준) n년 범위의 minYyyymm 계산: 예) years=1 -> 1년치(12개월) */
    public static String minYyyymmForYears(String maxYyyymm, int years) {
        if (years <= 0) throw new IllegalArgumentException("years must be positive");
        return minYyyymmForMonths(maxYyyymm, years * 12);
    }
}