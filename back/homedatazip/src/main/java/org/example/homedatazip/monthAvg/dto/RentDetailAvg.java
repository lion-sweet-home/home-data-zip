package org.example.homedatazip.monthAvg.dto;

import org.example.homedatazip.monthAvg.entity.MonthAvg;

public record RentDetailAvg (
        Long jeonseDepositAvg,
        Long wolseDepositAvg,
        Long wolseRentAvg,
        Integer jeonseCount,
        Integer wolseCount,
        String yyyymm
){
    public static RentDetailAvg from(MonthAvg m){
        return new RentDetailAvg (
                getJeonseAvg(m),
                getWolseAvg(m),
                getWolseRentAvg(m),
                m.getJeonseCount(),
                m.getWolseCount(),
                m.getYyyymm()
        );
    }

    private static Long getJeonseAvg(MonthAvg monthAvg){
        return monthAvg.getJeonseCount() == 0 ? 0: monthAvg.getJeonseDepositSum() /monthAvg.getJeonseCount();
    }
    private static Long getWolseAvg(MonthAvg monthAvg){
        return monthAvg.getWolseCount() == 0 ? 0:  monthAvg.getWolseDepositSum() /monthAvg.getWolseCount();
    }
    private static Long getWolseRentAvg(MonthAvg monthAvg){
        return monthAvg.getWolseCount() == 0 ? 0:  monthAvg.getWolseRentSum() /monthAvg.getWolseCount();
    }
}
