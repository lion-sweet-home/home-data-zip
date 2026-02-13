package org.example.homedatazip.tradeSale.dto;

import com.querydsl.core.annotations.QueryProjection;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public record SaleSearchResponse(
        String aptNm,
        String address,     // 아파트 주소
        Long dealAmount,    // 매매가
        Double exurArea,    // 면적
        String dealDate,
        Integer buildYear,
        Integer floor,
        Double latitude,
        Double longitude
) {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    @QueryProjection
    public SaleSearchResponse(
            String aptNm,
            String fullAddress,
            Long dealAmount,
            Double exurArea,
            LocalDate dealDate,
            Integer buildYear,
            Integer floor,
            Double latitude,
            Double longitude
    ) {
        this(
                aptNm,
                fullAddress,
                dealAmount,
                exurArea,
                dealDate != null ? dealDate.format(DATE_FORMATTER) : null,
                buildYear,
                floor,
                latitude,
                longitude
        );
    }

}
