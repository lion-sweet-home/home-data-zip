package org.example.homedatazip.busstation.client.dto;

import java.util.List;

// 응답(JSON) -> DTO 파싱
public record SeoulBusStopResponse(
        BusStopLocationXyInfo busStopLocationXyInfo
) {
    public record BusStopLocationXyInfo(
            int list_total_count,
            Result RESULT,
            List<Row> row
    ) {}

    public record Result(
            String CODE,
            String MESSAGE
    ) {}

    public record Row(
            String STOPS_NO, // ARS-ID (버스 정류장 전화/안내용 고유번호)
            String STOPS_NM, // 정류소명
            String XCRD, // 경도
            String YCRD, // 위도
            String NODE_ID
    ) {}
}
