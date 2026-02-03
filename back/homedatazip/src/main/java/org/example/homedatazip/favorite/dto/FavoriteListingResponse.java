package org.example.homedatazip.favorite.dto;

import org.example.homedatazip.listing.type.TradeType;

import java.time.LocalDateTime;

/** 관심 매물 목록 API 응답 DTO */
public record FavoriteListingResponse(
        Long favoriteId,
        Long listingId,
        String aptName,
        String roadAddress,
        String jibunAddress,
        TradeType tradeType,
        Double exclusiveArea,
        Integer floor,
        Long salePrice,
        Long deposit,
        Integer monthlyRent,
        String contactPhone,
        LocalDateTime favoritedAt
) {}
