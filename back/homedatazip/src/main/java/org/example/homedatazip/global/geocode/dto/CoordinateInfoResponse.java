package org.example.homedatazip.global.geocode.dto;

import lombok.AccessLevel;
import lombok.Builder;
import org.example.homedatazip.data.Region;

@Builder(access = AccessLevel.PRIVATE)
public record CoordinateInfoResponse(
        Region region,
        String jibunAddress, // 지번주소
        String roadAddress, // 도로명주소
        Double latitude, // 위도
        Double longitude // 경도
) {
    public static CoordinateInfoResponse create(Region region, String jibunAddress, String roadAddress,
                                                Double latitude, Double longitude) {
        return CoordinateInfoResponse.builder()
                .region(region)
                .jibunAddress(jibunAddress)
                .roadAddress(roadAddress)
                .latitude(latitude)
                .longitude(longitude)
                .build();
    }
}
