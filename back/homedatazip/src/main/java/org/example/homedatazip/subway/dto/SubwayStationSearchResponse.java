package org.example.homedatazip.subway.dto;

import java.util.List;

/** 지하철 역 검색 API 전체 응답 */
public record SubwayStationSearchResponse(
        List<SubwayStationResponse> stations
) {}
