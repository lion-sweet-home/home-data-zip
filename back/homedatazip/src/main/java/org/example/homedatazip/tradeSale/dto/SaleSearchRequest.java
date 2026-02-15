package org.example.homedatazip.tradeSale.dto;

import jakarta.validation.constraints.NotBlank;

public record   SaleSearchRequest(
        @NotBlank(message = "시/도는 필수 선택 사항입니다.")
        String sido,
        @NotBlank(message = "구/군은 필수 선택 사항입니다.")
        String gugun,
        String dong,
        String keyword,

        Long minAmount,
        Long maxAmount,
        Integer periodMonths,
        Double minArea,
        Double maxArea,
        Integer minBuildYear,
        Integer maxBuildYear
) {}
