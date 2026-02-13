package org.example.homedatazip.monthAvg.dto;

import org.example.homedatazip.monthAvg.entity.MonthAvg;

import java.util.List;

public record MonthAvgRentDepositResponse(
        Long id,
        Long aptId,
        Double exclusive,
        String yyyymm,
        Long jeonseAvg,
        Long wolseAvg,
        Long wolseRentAvg,
        Integer jeonseCount,
        Integer wolseCount
) {

    public static MonthAvgRentDepositResponse from(MonthAvg monthAvg) {
        int jc = nz(monthAvg.getJeonseCount());
        int wc = nz(monthAvg.getWolseCount());

        long jeonseAvg = avg(nz(monthAvg.getJeonseDepositSum()), jc);
        long wolseAvg = avg(nz(monthAvg.getWolseDepositSum()), wc);
        long wolseRentAvg = avg(nz(monthAvg.getWolseRentSum()), wc);

        return new MonthAvgRentDepositResponse(
                monthAvg.getId(),
                monthAvg.getAptId(),
                (monthAvg.getAreaTypeId() % 1_000_000) / 100.0,
                monthAvg.getYyyymm(),
                jeonseAvg,
                wolseAvg,
                wolseRentAvg,
                jc,
                wc
        );
    }

    public static List<MonthAvgRentDepositResponse> fromList(List<MonthAvg> monthAvgs) {
        return monthAvgs.stream()
                .map(MonthAvgRentDepositResponse::from)
                .toList();
    }

    private static long avg(long sum, int count) {
        return count <= 0 ? 0L : (sum / count);
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }

    private static long nz(Long v) {
        return v == null ? 0L : v;
    }
}
