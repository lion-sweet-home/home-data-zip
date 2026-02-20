package org.example.homedatazip.apartment.dto;

public record AptSummaryResponse(
        Long aptId, // 아파트 id
        String aptName, // 아파트 이름
        String gu, // 구
        String dong,
        Double areaTypeId, // 평수
        Long AvgDealAmount, // 평균 거래가
        Double priceChangeRate, // 등략률
        Integer tradeCount // 거래량
) {
}
