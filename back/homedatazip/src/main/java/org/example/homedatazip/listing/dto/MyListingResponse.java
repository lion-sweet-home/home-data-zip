package org.example.homedatazip.listing.dto;

import org.example.homedatazip.listing.type.TradeType;

import java.time.LocalDateTime;

public record MyListingResponse(
        Long listingId,
        Long apartmentId,
        String apartmentName,
        Integer buildYear,          // Apartment에서 가져옴 (없으면 null)
        TradeType tradeType,

        //전용면적
        Double exclusiveArea,
        Integer floor,

        Long salePrice,
        Long deposit,
        Integer monthlyRent,

        String contactPhone,
        String description,

        String status,
        LocalDateTime createdAt,
        String mainImageUrl
) {
}
