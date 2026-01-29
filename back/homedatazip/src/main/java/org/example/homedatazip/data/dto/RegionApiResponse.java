package org.example.homedatazip.data.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RegionApiResponse(
        @JsonProperty("법정동코드")
        String lawdCode,
        @JsonProperty("시도명")
        String sido,
        @JsonProperty("시군구명")
        String gugun,
        @JsonProperty("읍면동명")
        String dong,
        @JsonProperty("삭제일자")
        String deletedAt
) {}
