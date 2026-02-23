package org.example.homedatazip.apartment.dto;

import com.querydsl.core.annotations.QueryProjection;
import org.example.homedatazip.apartment.entity.Apartment;

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

    public static MarkResponse from(Apartment apt){
        return new MarkResponse(
                apt.getId(),
                apt.getAptName(),
                apt.getLatitude(),
                apt.getLongitude()
        );
    }
}
