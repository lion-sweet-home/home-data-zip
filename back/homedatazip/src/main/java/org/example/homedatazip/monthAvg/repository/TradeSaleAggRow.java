package org.example.homedatazip.monthAvg.repository;

public interface TradeSaleAggRow {
    Long getAptId();
    String getYyyymm();
    Long getAreaTypeId();

    Long getSaleDealAmountSum();
    Integer getSaleCount();
}
