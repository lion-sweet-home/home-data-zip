package org.example.homedatazip.tradeSale.dto;

import com.querydsl.core.annotations.QueryProjection;

public record TradeVolumeDto(
        String month,
        Long count
) {
    @QueryProjection
    public TradeVolumeDto {}
}
