package org.example.homedatazip.subway.dto;

import java.util.List;

/**
 * 지하철 역 검색 API 응답 DTO.
 * 역명 단위로 그룹핑하여, 해당 역의 호선 목록과 대표 좌표를 반환한다.
 *
 * @param latitude  위도 (DB 상 비 null)
 * @param longitude 경도 (DB 상 비 null)
 */
public record SubwayStationResponse(
        Long stationId,
        String stationName,
        List<String> lineNames,
        Double latitude,
        Double longitude
) {}
