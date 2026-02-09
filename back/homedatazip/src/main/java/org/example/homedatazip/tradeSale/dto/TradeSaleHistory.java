package org.example.homedatazip.tradeSale.dto;

import com.querydsl.core.annotations.QueryProjection;

public record TradeSaleHistory(
        Long dealAmount,
        String buildYear,
        Integer floor,
        String dealDate,
        Double exurArea,
        Double areaKey,
        Long areaTypeId
) {
    @QueryProjection
    public TradeSaleHistory(Long dealAmount, String buildYear, Integer floor, String dealDate, Double exurArea, Double areaKey, Long areaTypeId) {
        this.dealAmount = dealAmount;
        this.buildYear = buildYear;
        this.floor = floor;
        this.dealDate = dealDate;
        this.exurArea = exurArea;
        this.areaKey = areaKey;
        this.areaTypeId = areaTypeId;
    }
}
