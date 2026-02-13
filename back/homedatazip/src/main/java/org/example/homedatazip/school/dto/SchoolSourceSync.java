package org.example.homedatazip.school.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 학교 OpenAPI로부터 수신하는 Raw 데이터 매핑용 DTO
 */
/**
 * record를 사용한 학교 데이터 수집용 DTO
 */
public record  SchoolSourceSync(
        @JsonProperty("학교ID") String schoolId,
        @JsonProperty("학교명") String schoolName,
        @JsonProperty("학교급구분") String schoolLevel,
        @JsonProperty("소재지도로명주소") String roadAddress,
        @JsonProperty("위도") Double latitude,
        @JsonProperty("경도") Double longitude
) {}