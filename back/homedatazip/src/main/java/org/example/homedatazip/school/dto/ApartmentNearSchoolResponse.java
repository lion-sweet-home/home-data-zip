package org.example.homedatazip.school.dto;

/** 학교 반경 내 아파트 검색 API 응답 DTO */
public record ApartmentNearSchoolResponse(
        Long apartmentId,
        String aptName,
        String roadAddress,
        String jibunAddress,
        Double latitude,
        Double longitude,
        Integer buildYear,
        Double distanceKm
) {}