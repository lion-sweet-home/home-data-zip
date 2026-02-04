package org.example.homedatazip.monthAvg.dto;

import org.example.homedatazip.monthAvg.entity.MonthAvg;

import java.util.List;

public record MonthTotalTradeResponse (
        List<MonthTotalTrade> monthTotalTrades
){
    public static MonthTotalTradeResponse map(List<MonthAvg> avgs){
        List<MonthTotalTrade> list = avgs.stream()
                .map(MonthTotalTrade::from)
                .toList();
        return new MonthTotalTradeResponse(list);
    }

    public record MonthTotalTrade(
            Long id,
            Long aptId,
            String yyyymm,
            Integer jeonseCount,
            Integer wolseCount
    ){
        public static MonthTotalTrade from(MonthAvg monthAvg){
            return new MonthTotalTrade(
                    monthAvg.getId(),
                    monthAvg.getAptId(),
                    monthAvg.getYyyymm(),
                    monthAvg.getJeonseCount(),
                    monthAvg.getWolseCount()
            );
        }
    }
}
