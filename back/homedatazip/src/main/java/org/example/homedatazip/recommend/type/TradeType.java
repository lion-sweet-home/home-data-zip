package org.example.homedatazip.recommend.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TradeType {
    SALE("매매"),
    RENT("전세"),
    WOLSE("월세");

    private final String description;

    public static TradeType of(Long monthlyRent, boolean isRent) {
        if (!isRent) return SALE;
        return (monthlyRent == null || monthlyRent == 0) ? RENT : WOLSE;
    }
}
