package org.example.homedatazip.global.geocode.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record GeoCoordinateResponse(
        List<Document> documents
) {
    public record Document(
            Address address,
            @JsonProperty("road_address") RoadAddress roadAddress,
            @JsonProperty("address_name") String addressName,
            Double x, // 키워드 검색용 경도
            Double y  // 키워드 검색용 위도
    ) {
    }

    public record Address(
            @JsonProperty("address_name") String addressName, // 지번 주소
            @JsonProperty("b_code") String bCode, // 10자리 법정동 코드
            @JsonProperty("x") Double longitude, // 경도
            @JsonProperty("y") Double latitude // 위도
    ) {
    }

    public record RoadAddress(
            @JsonProperty("address_name") String roadAddressName, // 도로명 주소
            @JsonProperty("x") Double longitude, // 경도
            @JsonProperty("y") Double latitude // 위도
    ) {
    }
}
