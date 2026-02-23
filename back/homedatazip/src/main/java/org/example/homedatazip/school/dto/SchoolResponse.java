package org.example.homedatazip.school.dto;

import org.example.homedatazip.school.entity.School;

/** 지역 검색 시 학교 목록 */
public record SchoolResponse(
        Long id,
        String name,
        String schoolLevel,
        String roadAddress,
        Double latitude,
        Double longitude
) {
    public static SchoolResponse from(School school) {
        return new SchoolResponse(
                school.getId(),
                school.getName(),
                school.getSchoolLevel(),
                school.getRoadAddress(),
                school.getLatitude(),
                school.getLongitude()
        );
    }
}
