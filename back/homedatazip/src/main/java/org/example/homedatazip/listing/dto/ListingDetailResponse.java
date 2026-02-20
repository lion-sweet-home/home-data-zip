package org.example.homedatazip.listing.dto;

import org.example.homedatazip.listing.type.RentType;
import org.example.homedatazip.listing.type.TradeType;

import java.time.LocalDateTime;
import java.util.List;

public record ListingDetailResponse(
        Long id,
        String title,
        List<ListingImageResponse> images,

        // 추가 권장 필드:
        TradeType tradeType,
        RentType rentType,
        Double exclusiveArea,
        Integer floor,
        Long salePrice,
        Long deposit,
        Integer monthlyRent,
        Integer buildYear,
        String description,
        String contactPhone,
        LocalDateTime createdAt,
        String jibunAddress
) {}