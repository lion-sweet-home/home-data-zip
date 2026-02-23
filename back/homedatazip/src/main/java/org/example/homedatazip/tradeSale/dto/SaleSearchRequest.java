package org.example.homedatazip.tradeSale.dto;

public record   SaleSearchRequest(
        String sido,
        String gugun,
        String dong,
        String keyword,

        Long minAmount,
        Long maxAmount,
        Integer periodMonths,
        Double minArea,
        Double maxArea,
        Integer minBuildYear,
        Integer maxBuildYear,

        // 지도 bounds 기반 마커 갱신용 (선택)
        Integer level,
        Integer limit,
        Double east,
        Double west,
        Double north,
        Double south
) {}
