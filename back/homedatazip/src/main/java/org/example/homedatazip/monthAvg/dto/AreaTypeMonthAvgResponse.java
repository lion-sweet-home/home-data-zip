package org.example.homedatazip.monthAvg.dto;

import org.example.homedatazip.monthAvg.entity.MonthAvg;

import java.util.List;

public record AreaTypeMonthAvgResponse (
        Long jeonseAvg,
        Long wolseAvg,
        Long wolseRentAvg

){
    public static AreaTypeMonthAvgResponse map(List<MonthAvg> ms){
        return new AreaTypeMonthAvgResponse(
                getJeonseAvg(ms),
                getWolseAvg(ms),
                getWolseRentAvg(ms)
        );
    }

    private static Long getJeonseAvg(List<MonthAvg> ms){
        Long jeonse = ms.stream().mapToLong(MonthAvg::getJeonseDepositSum).sum();
        Long sumCount = ms.stream().mapToLong(MonthAvg::getJeonseCount).sum();

        return sumCount == 0 ? 0L :jeonse / sumCount;
    }
    private static Long getWolseAvg(List<MonthAvg> ms){
        Long w = ms.stream().mapToLong(MonthAvg::getWolseDepositSum).sum();
        Long c = ms.stream().mapToLong(MonthAvg::getWolseCount).sum();
        return c == 0 ? 0 :  w/c;
    }
    private static Long getWolseRentAvg(List<MonthAvg> ms){
        Long w = ms.stream().mapToLong(MonthAvg::getWolseRentSum).sum();
        Long c = ms.stream().mapToLong(MonthAvg::getWolseCount).sum();
        return c == 0 ? 0 :  w/c;
    }
}
