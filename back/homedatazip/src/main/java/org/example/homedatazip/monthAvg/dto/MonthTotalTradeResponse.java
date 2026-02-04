package org.example.homedatazip.monthAvg.dto;

import org.example.homedatazip.monthAvg.entity.MonthAvg;

import java.util.List;

public record MonthTotalTradeResponse(
        Long aptId,
        String yyyymm,
        Integer jeonseCount,
        Integer wolseCount
){
    public static MonthTotalTradeResponse from(MonthAvg monthAvg){
        return new MonthTotalTradeResponse(
                monthAvg.getId(),
                monthAvg.getYyyymm(),
                monthAvg.getJeonseCount(),
                monthAvg.getWolseCount()
        );
    }
}

