package org.example.homedatazip.hospital.dto;

import java.util.Map;

public record HospitalStatsResponse(
        String sido,
        String gugun,
        String dong,
        Long totalCount,
        Map<String, Long> countByTypeName
) {
}
