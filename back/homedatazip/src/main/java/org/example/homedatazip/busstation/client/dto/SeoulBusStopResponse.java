package org.example.homedatazip.busstation.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SeoulBusStopResponse(
        @JsonProperty("busStopLocationXyInfo")
        BusStopLocationXyInfo busStopLocationXyInfo
) {

    public record BusStopLocationXyInfo(
            @JsonProperty("list_total_count")
            int listTotalCount,

            @JsonProperty("RESULT")
            Result result,

            @JsonProperty("row")
            List<Row> row
    ) {}

    public record Result(
            @JsonProperty("CODE")
            String code,
            @JsonProperty("MESSAGE")
            String message
    ) {}

    public record Row(
            @JsonProperty("STOPS_NO") String STOPS_NO,     // 정류소 고유번호(ARS-ID)
            @JsonProperty("STOPS_NM") String STOPS_NM,     // 정류소명
            @JsonProperty("XCRD") String XCRD,            // 경도
            @JsonProperty("YCRD") String YCRD,            // 위도
            @JsonProperty("NODE_ID") String NODE_ID,       // 노드ID
            @JsonProperty("STOPS_TYPE") String STOPS_TYPE  // 타입(있을 수도, 없을 수도)
    ) {}
}
