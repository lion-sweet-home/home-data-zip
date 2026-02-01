package org.example.homedatazip.subway.batch.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** OpenAPI JSON 역직렬화 + subway_station_sources Writer 공용 DTO */
public record SubwayStationSourceSync(
        @JsonProperty("outStnNum") String lineStationCode,
        @JsonProperty("stnKrNm") String stationName,
        @JsonProperty("lineNm") String lineName,
        @JsonProperty("convY") Double latitude,
        @JsonProperty("convX") Double longitude
) {}
