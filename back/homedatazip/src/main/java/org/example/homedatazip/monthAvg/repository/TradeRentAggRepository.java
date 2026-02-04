package org.example.homedatazip.monthAvg.repository;

import org.example.homedatazip.tradeRent.entity.TradeRent;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface TradeRentAggRepository extends Repository<TradeRent, Long> {

    //쿼리문은 하나씩 나가는데 어떻게 set으로 다수를 받아서 처리하고 List로 또 가져올 수 있는지..
    @Query(value = """
        SELECT
            t.aptId AS aptId,
            t.yyyymm AS yyyymm,
            (t.aptId * 1000000 + t.areaKey) AS areaTypeId,

            SUM(CASE WHEN t.monthlyRent = 0 THEN t.deposit ELSE 0 END) AS jeonseDepositSum,
            SUM(CASE WHEN t.monthlyRent <> 0 THEN t.deposit ELSE 0 END) AS wolseDepositSum,
            SUM(CASE WHEN t.monthlyRent <> 0 THEN t.monthlyRent ELSE 0 END) AS wolseRentSum,

            SUM(CASE WHEN t.monthlyRent = 0 THEN 1 ELSE 0 END) AS jeonseCount,
            SUM(CASE WHEN t.monthlyRent <> 0 THEN 1 ELSE 0 END) AS wolseCount
                 
        FROM (
            SELECT
                tr.apartment_id AS aptId,
                DATE_FORMAT(tr.deal_date, '%Y%m') AS yyyymm,
                ROUND(tr.exclusive_area * 10) AS areaKey,
                tr.deposit AS deposit,
                tr.monthly_rent AS monthlyRent
            FROM trade_rent tr
            WHERE tr.apartment_id IN (:aptIds)
              AND DATE_FORMAT(tr.deal_date, '%Y%m') IN (:yyyymms)
        ) t
        GROUP BY t.aptId, t.yyyymm, t.areaKey
        """, nativeQuery = true)
    List<TradeRentAggRow> aggregateByAptMonthAndArea(
            @Param("aptIds") Set<Long> aptIds,
            @Param("yyyymms") Set<String> yyyymms
    );
}