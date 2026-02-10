package org.example.homedatazip.apartment.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.homedatazip.apartment.dto.AptSaleAggregation;
import org.example.homedatazip.monthAvg.entity.QMonthAvg;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ApartmentSearchRepositoryImpl implements ApartmentSearchRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 여러 아파트의 기간별 매매 집계 조회
     * (6개월, 전전월, 전월 한 번에)
     */
    @Override
    public List<AptSaleAggregation> findSaleAggregationByAptIds(
            List<Long> aptIds,
            String sixMonthsAgo,
            String twoMonthsAgo,
            String lastMonth
    ) {
        QMonthAvg monthAvg = QMonthAvg.monthAvg;

        return queryFactory
                .select(Projections.constructor( // 조회 결과를 DTO 생성자에 매핑
                                AptSaleAggregation.class,
                                monthAvg.aptId,

                                // 6개월 집계
                                monthAvg.saleDealAmountSum.sum(),
                                monthAvg.saleCount.sum().longValue(),

                                // 전전월 집계
                                new CaseBuilder()
                                        .when(monthAvg.yyyymm.eq(twoMonthsAgo))
                                        .then(monthAvg.saleDealAmountSum)
                                        .otherwise(0L)
                                        .sum(),
                                new CaseBuilder()
                                        .when(monthAvg.yyyymm.eq(twoMonthsAgo))
                                        .then(monthAvg.saleCount.longValue())
                                        .otherwise(0L)
                                        .sum(),

                                // 전월 집계
                                new CaseBuilder()
                                        .when(monthAvg.yyyymm.eq(lastMonth))
                                        .then(monthAvg.saleDealAmountSum)
                                        .otherwise(0L)
                                        .sum(),
                                new CaseBuilder()
                                        .when(monthAvg.yyyymm.eq(lastMonth))
                                        .then(monthAvg.saleCount.longValue())
                                        .otherwise(0L)
                                        .sum()
                        )
                )
                .from(monthAvg)
                .where(
                        monthAvg.aptId.in(aptIds),
                        monthAvg.yyyymm.between(sixMonthsAgo, lastMonth)
                )
                .groupBy(monthAvg.aptId)
                .fetch();
    }
}
