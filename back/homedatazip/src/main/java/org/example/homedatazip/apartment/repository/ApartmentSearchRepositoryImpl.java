package org.example.homedatazip.apartment.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.homedatazip.apartment.dto.AptSaleAggregation;
import org.example.homedatazip.monthAvg.entity.MonthAvg;
import org.example.homedatazip.monthAvg.entity.QMonthAvg;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ApartmentSearchRepositoryImpl implements ApartmentSearchRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 여러 아파트의 매매 집계 조회
     * <br/>
     * 1. 전월 ~ 4년 전 데이터 조회 (날짜 내림차순)
     * 2. aptId + areaTypeId 기준 그룹핑
     * 3. 각 그룹에서 전월 데이터 + 비교 대상월 데이터 추출 => DTO 생성
     */
    @Override
    public List<AptSaleAggregation> findSaleAggregationByAptIds(
            List<Long> aptIds,
            String lastMonth,
            String searchMonth
    ) {
        QMonthAvg monthAvg = QMonthAvg.monthAvg;

        // 1. 전월 ~ 4년 전 데이터 조회 (yyyymm 내림차순 정렬)
        List<MonthAvg> allData = queryFactory
                .selectFrom(monthAvg)
                .where(
                        monthAvg.aptId.in(aptIds),
                        monthAvg.yyyymm.loe(lastMonth), // <=
                        monthAvg.yyyymm.goe(searchMonth) // >=
                )
                .orderBy(monthAvg.yyyymm.desc())
                .fetch();

        // 2. aptId + areaTypeId 기준 그룹핑
        Map<String, List<MonthAvg>> grouped = allData.stream()
                .collect(Collectors.groupingBy(
                                ma
                                        -> ma.getAptId() + "_" + ma.getAreaTypeId()
                        )
                );

        // 3. 각 그룹에서 전월 데이터 + 비교 대상월 데이터 추출
        List<AptSaleAggregation> result = new ArrayList<>();

        for (List<MonthAvg> group : grouped.values()) {
            // 전월 데이터 찾기
            MonthAvg lastMonthAvg = group.stream()
                    .filter(ma -> ma.getYyyymm().equals(lastMonth))
                    .findFirst()
                    .orElse(null);

            // 전월 데이터 없으면 스킵
            if (lastMonthAvg == null) {
                continue;
            }

            // 비교 대상월 찾기
            MonthAvg compareMonthAvg = group.stream()
                    .filter(ma -> ma.getYyyymm().compareTo(lastMonth) < 0)
                    .filter(ma -> ma.getSaleCount() > 0)
                    .findFirst() // 위에서 이미 정렬했으므로 첫 번째가 가장 최신
                    .orElse(null);

            // DTO 생성
            result.add(new AptSaleAggregation(
                            lastMonthAvg.getAptId(),
                            lastMonthAvg.getAreaTypeId(),
                            lastMonthAvg.getSaleDealAmountSum(),
                            lastMonthAvg.getSaleCount().longValue(),

                            compareMonthAvg != null
                                    ? compareMonthAvg.getYyyymm()
                                    : null,

                            compareMonthAvg != null
                                    ? compareMonthAvg.getSaleDealAmountSum()
                                    : null,

                            compareMonthAvg != null
                                    ? compareMonthAvg.getSaleCount().longValue()
                                    : null
                    )
            );
        }

        return result;
    }
}
