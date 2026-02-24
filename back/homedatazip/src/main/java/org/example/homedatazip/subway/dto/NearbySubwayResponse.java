package org.example.homedatazip.subway.dto;

import java.util.List;

/** 아파트 기준 가까운 지하철역 API 응답 DTO */
public record NearbySubwayResponse(
        String stationName,
        List<String> lineNames,
        double distanceKm,
        Double latitude,
        Double longitude
) {}
