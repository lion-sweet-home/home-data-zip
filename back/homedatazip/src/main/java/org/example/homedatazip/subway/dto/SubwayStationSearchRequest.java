package org.example.homedatazip.subway.dto;

/** 지하철 역 검색 요청 (한 검색란에 역명 또는 호선, 둘 다 optional) */
public record SubwayStationSearchRequest(
        String stationName,
        String lineName
) {}
