package org.example.homedatazip.tradeSale.dto;

import com.querydsl.core.annotations.QueryProjection;

public record DongRankResponse(
        String dong,
        Long tradeCount
    ) {
    @QueryProjection
    public DongRankResponse(String dong, Long tradeCount) {
        this.dong = dong;
        this.tradeCount = tradeCount;
    }
}
