package org.example.homedatazip.monthAvg.dto;

import org.example.homedatazip.monthAvg.entity.MonthAvg;


public record MonthTotalTradeAreaResponse(
        Long aptId,
        String yyyymm,
        Long areaKey,
        Double exclusive,
        Integer jeonseCount,
        Integer wolseCount
){
    public static MonthTotalTradeAreaResponse from(MonthAvg monthAvg){
        return new MonthTotalTradeAreaResponse(
                monthAvg.getAptId(),
                monthAvg.getYyyymm(),
                getAreaKey(monthAvg),
                getExclusive(monthAvg),
                monthAvg.getJeonseCount(),
                monthAvg.getWolseCount()
            );
        }
    private static Long getAreaKey(MonthAvg monthAvg){
        Long areaTypeId = monthAvg.getAreaTypeId();
        return areaTypeId % 1_000_000;
    }
    private static Double getExclusive(MonthAvg monthAvg){
        Long areaKey1 = getAreaKey(monthAvg);
        double v = areaKey1 / 100.0;
        return v;
    }

}