package org.example.homedatazip.listing.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.example.homedatazip.listing.type.TradeType;

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

        String description
) {
}
