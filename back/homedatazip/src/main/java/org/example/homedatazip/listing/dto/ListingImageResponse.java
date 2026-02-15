package org.example.homedatazip.listing.dto;

public record ListingImageResponse(
        Long id,
        String url,
        boolean isMain,
        Integer sortOrder
) {}