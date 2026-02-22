package org.example.homedatazip.apartment.dto;

/**
 * 지도 클러스터(집계) 응답 DTO
 * - latitude/longitude: 해당 클러스터를 대표하는 좌표(예: 평균값)
 * - count: 해당 클러스터에 포함된 개별 포인트(아파트) 개수
 */
public record MarkerClusterResponse(
        Double latitude,
        Double longitude,
        Long count
) {
}

