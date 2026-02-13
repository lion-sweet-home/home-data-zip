package org.example.homedatazip.tradeSale.dto;

import com.querydsl.core.annotations.QueryProjection;

public record RecentTradeSale(
        Long dealAmount,
        Double exurArea,
        String dealDate,
        Integer floor
) {
    @QueryProjection
    public RecentTradeSale(Long dealAmount, Double exurArea, String dealDate, Integer floor) {
        this.dealAmount = dealAmount;
        this.exurArea = exurArea;
        this.dealDate = dealDate;
        this.floor = floor;
    }
}
