package org.example.homedatazip.recommend.dto;

import lombok.Builder;
import org.example.homedatazip.recommend.type.TradeType;

@Builder
public record UserSearchLogRequest(
        String sggCode,
        Long price,
        Long rentAmount,
        Double area,
        TradeType tradeType,

        Long minPrice,
        Long maxPrice,
        Double minArea,
        Double maxArea,
        Integer buildYearFilter,
        Integer periodMonths
) {}