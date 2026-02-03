package org.example.homedatazip.listing.dto;

import org.example.homedatazip.listing.type.RentType;
import org.example.homedatazip.listing.type.TradeType;

import java.time.LocalDateTime;

public record ListingSearchResponse(
        Long listingId,

        Long regionId,
        Long apartmentId,
        String apartmentName,
        Integer buildYear,

        TradeType tradeType,
        RentType rentType,       // RENT일 때만: CHARTER | MONTHLY

        Double exclusiveArea,
        Integer floor,

        Long salePrice,
        Long deposit,
        Integer monthlyRent,

        String description,
        LocalDateTime createdAt
) {
}
