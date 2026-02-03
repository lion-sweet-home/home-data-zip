package org.example.homedatazip.listing.dto;

import java.time.LocalDateTime;

public record ListingCreateResponse(
        Long listingId,
        String status,
        LocalDateTime createdAt
) {
    public static ListingCreateResponse of(Long listingId, String status, LocalDateTime createdAt) {
        return new ListingCreateResponse(listingId, status, createdAt);
    }
}
