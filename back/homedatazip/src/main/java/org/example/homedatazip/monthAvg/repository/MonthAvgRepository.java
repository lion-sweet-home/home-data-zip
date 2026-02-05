// file: src/main/java/org/example/homedatazip/monthavg/repository/MonthAvgRepository.java
package org.example.homedatazip.monthAvg.repository;

import org.example.homedatazip.monthAvg.dto.MonthTotalTradeResponse;
import org.example.homedatazip.monthAvg.entity.MonthAvg;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MonthAvgRepository extends JpaRepository<MonthAvg, Long> {

    @Query("""
    select new org.example.homedatazip.monthAvg.dto.MonthTotalTradeResponse(
        :aptId,
        m.yyyymm,
        sum(coalesce(m.jeonseCount, 0)),
        sum(coalesce(m.wolseCount, 0))
    )
    from MonthAvg m
    where m.aptId = :aptId
      and m.yyyymm between :minYyyymm and :maxYyyymm
    group by m.yyyymm
    order by m.yyyymm asc
""")
    List<MonthTotalTradeResponse> findMonthlyTotals(
            @Param("aptId") long aptId,
            @Param("minYyyymm") String minYyyymm,
            @Param("maxYyyymm") String maxYyyymm
    );

    List<MonthAvg> findAllByAptIdAndAreaTypeIdAndYyyymmBetweenOrderByYyyymmAsc(
            long aptId,
            long areaTypeId,
            String minYyyymm,
            String maxYyyymm
    );

    @Modifying
    @Query(value = """
        INSERT INTO month_avg (
            apt_id, yyyymm, area_type_id,
            sale_deal_amount_sum,
            jeonse_deposit_sum,
            wolse_deposit_sum, wolse_rent_sum,
            sale_count, jeonse_count, wolse_count,
            updated_at
        )
        VALUES (
            :aptId, :yyyymm, :areaTypeId,
            0,
            :jeonseDepositSum,
            :wolseDepositSum, :wolseRentSum,
            0, :jeonseCount, :wolseCount,
            CURRENT_TIMESTAMP
        )
        ON DUPLICATE KEY UPDATE
            jeonse_deposit_sum = VALUES(jeonse_deposit_sum),
            wolse_deposit_sum  = VALUES(wolse_deposit_sum),
            wolse_rent_sum     = VALUES(wolse_rent_sum),
            jeonse_count       = VALUES(jeonse_count),
            wolse_count        = VALUES(wolse_count),
            updated_at         = CURRENT_TIMESTAMP
        """, nativeQuery = true)
    void upsertReplaceRent(
            @Param("aptId") long aptId,
            @Param("yyyymm") String yyyymm,
            @Param("areaTypeId") long areaTypeId,
            @Param("jeonseDepositSum") long jeonseDepositSum,
            @Param("wolseDepositSum") long wolseDepositSum,
            @Param("wolseRentSum") long wolseRentSum,
            @Param("jeonseCount") int jeonseCount,
            @Param("wolseCount") int wolseCount
    );

    // Sale만 갱신 (전/월세 컬럼/카운트는 UPDATE에서 건드리지 않음)
    @Modifying
    @Query(value = """
        INSERT INTO month_avg (
            apt_id, yyyymm, area_type_id,
            sale_deal_amount_sum,
            jeonse_deposit_sum,
            wolse_deposit_sum, wolse_rent_sum,
            sale_count, jeonse_count, wolse_count,
            updated_at
        )
        VALUES (
            :aptId, :yyyymm, :areaTypeId,
            :saleDealAmountSum,
            0,
            0, 0,
            :saleCount, 0, 0,
            CURRENT_TIMESTAMP
        )
        ON DUPLICATE KEY UPDATE
            sale_deal_amount_sum = VALUES(sale_deal_amount_sum),
            sale_count           = VALUES(sale_count),
            updated_at           = CURRENT_TIMESTAMP
        """, nativeQuery = true)
    void upsertReplaceSale(
            @Param("aptId") long aptId,
            @Param("yyyymm") String yyyymm,
            @Param("areaTypeId") long areaTypeId,
            @Param("saleDealAmountSum") long saleDealAmountSum,
            @Param("saleCount") int saleCount
    );

    //area_type_id 리스트로 호출
    @Query("""
        select distinct m.areaTypeId
        from MonthAvg m
        where m.aptId = :aptId
        order by m.areaTypeId
    """)
    List<Long> findDistinctAreaTypeIdsByAptId(@Param("aptId") long aptId);

}