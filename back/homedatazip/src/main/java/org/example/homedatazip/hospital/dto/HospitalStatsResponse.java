package org.example.homedatazip.hospital.dto;

import java.util.Map;

public record HospitalStatsResponse(
        String dong,
        Long totalCount,
        Map<String, Long> countByTypeName
) {
}
