package org.example.homedatazip.global.geocode.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record GeoAddressResponse(
        List<Document> documents
) {
    public record Document(
            @JsonProperty("region_type") String regionType, // B (법정동) 또는 H (행정동)
            @JsonProperty("code") String bCode // 법 or 행정동 코드
    ) {
    }
}
