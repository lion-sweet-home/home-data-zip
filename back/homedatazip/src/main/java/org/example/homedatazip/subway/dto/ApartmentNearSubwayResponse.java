package org.example.homedatazip.subway.dto;

/**
 * 지하철 역 반경 내 아파트 검색 API 응답 DTO.
 */
public record ApartmentNearSubwayResponse(
        Long apartmentId,
        String aptName,
        String roadAddress,
        String jibunAddress,
        Double latitude,
        Double longitude,
        Integer buildYear,
        Double distanceKm
) {}
