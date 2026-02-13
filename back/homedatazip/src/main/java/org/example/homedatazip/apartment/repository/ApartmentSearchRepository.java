package org.example.homedatazip.apartment.repository;

import org.example.homedatazip.apartment.dto.AptSaleAggregation;

import java.util.List;

public interface ApartmentSearchRepository {

    /**
     * 전월 집계 + 비교 대상월 집계 조회
     * (아파트별, 평수별)
     */
    List<AptSaleAggregation> findSaleAggregationByAptIds(
            List<Long> aptIds,
            String lastMonth,
            String searchMonth
    );
}
