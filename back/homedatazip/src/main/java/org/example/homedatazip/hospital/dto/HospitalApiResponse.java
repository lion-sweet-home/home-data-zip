package org.example.homedatazip.hospital.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HospitalApiResponse(

        /**
         * 최상위 래퍼 객체
         */
        @JsonProperty("TbHospitalInfo")
        HospitalInfo hospitalInfo
) {

    /**
     * {
     *   "TbHospitalInfo": {
     *     "list_total_count": 22061,
     *     "RESULT": { ... },
     *     "row": [ ... ]
     *   }
     * }
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HospitalInfo(

            @JsonProperty("list_total_count")
            Integer listTotalCount,

            @JsonProperty("RESULT")
            Result result,

            @JsonProperty("row")
            List<HospitalRow> rows
    ) {
        @JsonCreator
        public HospitalInfo {}
    }

    /**
     * {
     *   "RESULT": {
     *     "CODE": "INFO-000",
     *     "MESSAGE": "정상 처리되었습니다"
     *   }
     * }
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result(

            @JsonProperty("CODE")
            String code,

            @JsonProperty("MESSAGE")
            String message
    ) {
        @JsonCreator
        public Result {}
    }

    /**
     * 개별 병원 데이터
     * <br/>
     * {
     *   "HPID": "A1120837",
     *   "DUTYNAME": "가산기대찬의원",
     *   "DUTYDIVNAM": "의원",
     *   "DUTYADDR": "서울특별시 금천구...",
     *   "WGS84LAT": 37.4803938036867,
     *   "WGS84LON": 126.88412249700781
     * }
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HospitalRow(

            @JsonProperty("HPID")
            String hospitalId,

            @JsonProperty("DUTYNAME")
            String name,

            @JsonProperty("DUTYDIVNAM")
            String typeName,

            @JsonProperty("DUTYADDR")
            String address,

            @JsonProperty("WGS84LAT")
            Double latitude,

            @JsonProperty("WGS84LON")
            Double longitude
    ) {
        @JsonCreator
        public HospitalRow {}
    }

    // ============================================================
    // 편의 메서드
    // ============================================================

    /**
     * 전체 데이터 건수 반환
     */
    public Integer getListTotalCount() {
        return hospitalInfo != null ? hospitalInfo.listTotalCount() : null;
    }

    /**
     * 병원 데이터 목록 반환
     */
    public List<HospitalRow> getRows() {
        return hospitalInfo != null ? hospitalInfo.rows() : null;
    }

    /**
     * API 응답 결과 반환
     */
    public Result getResult() {
        return hospitalInfo != null ? hospitalInfo.result() : null;
    }
}