package org.example.homedatazip.listing.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.example.homedatazip.listing.type.TradeType;

import java.util.List;

public record ListingCreateRequest(

        @NotNull
        Long apartmentId,

        @NotNull
        TradeType tradeType, // SALE or RENT

        @NotNull @Min(1)
        Double exclusiveArea,

        @NotNull
        Integer floor,

        // SALE일 때만 사용 (필수)
        Long salePrice,

        // RENT일 때 필수
        Long deposit,

        // RENT일 때 필수 (전세면 0, 월세면 1 이상)
        Integer monthlyRent,

        // null 가능
        String contactPhone,

        String description,

        // 이미지: temp 업로드 후 넘어오는 key들
        List<String> imageTempKeys,

        // 원본 파일명(확장자 추출용)
        List<String> imageOriginalNames,

        // 대표 이미지 인덱스 (null이면 0)
        Integer mainIndex
) {
}
