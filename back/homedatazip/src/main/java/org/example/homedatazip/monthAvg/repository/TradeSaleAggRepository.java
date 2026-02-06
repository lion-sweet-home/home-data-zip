package org.example.homedatazip.monthAvg.repository;

import org.example.homedatazip.tradeSale.entity.TradeSale;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface TradeSaleAggRepository extends Repository<TradeSale, Long> {

    @Query(value = """
        SELECT
            t.aptId AS aptId,
            t.yyyymm AS yyyymm,
            (t.aptId * 1000000 + t.areaKey) AS areaTypeId,

            SUM(t.dealAmount)  AS saleDealAmountSum,
            COUNT(*)      AS saleCount
        FROM (
            SELECT
                ts.apartment_id AS aptId,
                DATE_FORMAT(ts.deal_date, '%Y%m') AS yyyymm,
                ROUND(ts.exclusive_area * 100) AS areaKey,
                ts.deal_amount AS dealAmount
            FROM trade_sale ts
            WHERE ts.apartment_id IN (:aptIds)
              AND DATE_FORMAT(ts.deal_date, '%Y%m') IN (:yyyymms)
        ) t
        GROUP BY t.aptId, t.yyyymm, t.areaKey
        """, nativeQuery = true)
    List<TradeSaleAggRow> aggregateByAptMonthAndArea(
            @Param("aptIds") Set<Long> aptIds,
            @Param("yyyymms") Set<String> yyyymms
    );
}