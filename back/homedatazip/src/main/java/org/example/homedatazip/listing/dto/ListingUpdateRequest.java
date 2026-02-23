package org.example.homedatazip.listing.dto;

import java.util.List;

public record ListingUpdateRequest(
        String title,


        List<String> addImageTempKeys,
        List<String> addImageOriginalNames,


        List<Long> deleteImageIds,

        //대표 이미지
        Long mainImageId
) {}
