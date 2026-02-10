package org.example.homedatazip.school.dto;

/** 아파트 기준 가까운 학교 API 응답 DTO */
public record NearbySchoolResponse(
        String schoolName,
        String schoolLevel,
        double distanceKm
) {}
