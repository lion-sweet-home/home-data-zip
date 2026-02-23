package org.example.homedatazip.apartment.repository;

import org.example.homedatazip.apartment.dto.AptSaleAggregation;
import org.example.homedatazip.apartment.entity.Apartment;

import java.util.List;

public interface ApartmentSearchRepository {

    List<Apartment> findAptListContaining(
            String keyword,
            String sido,
            String gugun,
            String dong
    );

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
