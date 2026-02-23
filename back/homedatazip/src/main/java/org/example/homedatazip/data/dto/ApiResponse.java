package org.example.homedatazip.data.dto;

import java.util.List;

// API 응답 전체를 감싸는 record
public record ApiResponse(
        int page,
        int perPage,
        int totalCount,
        List<RegionApiResponse> data
) {}
