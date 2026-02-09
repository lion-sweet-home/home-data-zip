package org.example.homedatazip.apartment.repository;

import org.example.homedatazip.apartment.dto.AptSaleAggregation;

import java.util.List;

public interface ApartmentSearchRepository {

    List<AptSaleAggregation> findSaleAggregationByAptIds(
            List<Long> aptIds,
            String sixMonthsAgo,
            String twoMonthsAgo,
            String lastMonth
    );
}
