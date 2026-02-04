package org.example.homedatazip.monthAvg.repository;

public interface TradeRentAggRow {
    Long getAptId();
    String getYyyymm();
    Long getAreaTypeId(); // area_key 기반으로 만든 id
    Long getJeonseDepositSum();
    Long getWolseDepositSum();
    Long getWolseRentSum();
    Integer getJeonseCount();
    Integer getWolseCount();
}
