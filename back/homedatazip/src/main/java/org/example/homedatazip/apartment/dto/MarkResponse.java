package org.example.homedatazip.apartment.dto;

import com.querydsl.core.annotations.QueryProjection;

public record MarkResponse(
        Long aptId,
        String aptNm,
        Double latitude,
        Double longitude
) {
    @QueryProjection
    public MarkResponse(Long aptId, String aptNm, Double latitude, Double longitude) {
        this.aptId = aptId;
        this.aptNm = aptNm;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
