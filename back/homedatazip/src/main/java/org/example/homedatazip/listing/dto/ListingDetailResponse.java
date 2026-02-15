package org.example.homedatazip.listing.dto;

import java.util.List;

public record ListingDetailResponse(
        Long id,
        String title,
        List<ListingImageResponse> images
) {}