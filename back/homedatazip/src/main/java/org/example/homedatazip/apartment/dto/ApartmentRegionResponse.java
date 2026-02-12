package org.example.homedatazip.apartment.dto;

/**
 * aptId로 지역(시/도, 구/군, 동) 조회 응답 DTO
 * - SidePanner에서 병원/기타 주변시설 조회를 위해 사용
 */
public record ApartmentRegionResponse(
        String sido,
        String gugun,
        String dong
) {
}

