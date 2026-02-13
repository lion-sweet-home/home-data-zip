package org.example.homedatazip.monthAvg.repository.impl;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.homedatazip.apartment.entity.QApartment;
import org.example.homedatazip.data.QRegion;
import org.example.homedatazip.monthAvg.dto.MonthTop3SalePriceResponse;
import org.example.homedatazip.monthAvg.entity.QMonthAvg;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class SaleMonthAvgTop3RepositoryImpl implements SaleMonthAvgTop3Repository {

    private final JPAQueryFactory queryFactory;
    private static final DateTimeFormatter YM_FMT = DateTimeFormatter.ofPattern("yyyyMM");

    public record AptMonthAggRow(
            Long areaTypeId,
            Long aptId,
            Double exclusive,
            String aptName,
            String gugun,
            String dong,
            Integer countSum,
            Long dealAmountSum
    ) {}

    public record PrevAggRow(
            Long areaTypeId,
            Integer countSum,
            Long dealAmountSum
    ) {}

    @Override
    public List<MonthTop3SalePriceResponse> top3ByLastMonth(YearMonth lastMonth) {
        String lastYm = lastMonth.format(YM_FMT);
        String prevYm = lastMonth.minusMonths(1).format(YM_FMT);

        QMonthAvg m = QMonthAvg.monthAvg;
        QApartment a = QApartment.apartment;
        QRegion r = QRegion.region;

        // areaTypeId = aptId*1_000_000 + areaKey(=exclusive*100)
        NumberExpression<Long> exclusive100 = Expressions.numberTemplate(
                Long.class,
                "MOD({0}, 1000000)",
                m.areaTypeId
        );
        NumberExpression<Double> exclusiveVal = exclusive100.doubleValue().divide(100.0);

        NumberExpression<Integer> lastCountSum = m.saleCount.sum();
        NumberExpression<Long> lastDealAmountSum = m.saleDealAmountSum.sum();

        List<AptMonthAggRow> lastTop3 = queryFactory
                .select(Projections.constructor(
                        AptMonthAggRow.class,
                        m.areaTypeId,
                        m.aptId,
                        exclusiveVal,
                        a.aptName,
                        r.gugun,
                        r.dong,
                        lastCountSum,
                        lastDealAmountSum
                ))
                .from(m)
                .join(a).on(a.id.eq(m.aptId))
                .join(r).on(r.id.eq(a.region.id))
                .where(m.yyyymm.eq(lastYm))
                .groupBy(m.areaTypeId, m.aptId, a.aptName, r.gugun, r.dong)
                .orderBy(lastCountSum.desc(), m.areaTypeId.asc())
                .limit(3)
                .fetch();

        if (lastTop3.isEmpty()) return List.of();

        List<Long> topAreaTypeIds = lastTop3.stream().map(AptMonthAggRow::areaTypeId).toList();

        NumberExpression<Integer> prevCountSum = m.saleCount.sum();
        NumberExpression<Long> prevDealAmountSum = m.saleDealAmountSum.sum();

        List<PrevAggRow> prevAggRows = queryFactory
                .select(Projections.constructor(
                        PrevAggRow.class,
                        m.areaTypeId,
                        prevCountSum,
                        prevDealAmountSum
                ))
                .from(m)
                .where(m.yyyymm.eq(prevYm), m.areaTypeId.in(topAreaTypeIds))
                .groupBy(m.areaTypeId)
                .fetch();

        Map<Long, PrevAggRow> prevByAreaTypeId = prevAggRows.stream()
                .collect(Collectors.toMap(PrevAggRow::areaTypeId, x -> x));

        List<MonthTop3SalePriceResponse> result = new ArrayList<>();

        for (AptMonthAggRow last : lastTop3) {
            PrevAggRow prev = prevByAreaTypeId.get(last.areaTypeId);

            Long lastAvg = safeAvg(last.dealAmountSum, last.countSum);
            Long prevAvg = (prev == null) ? null : safeAvg(prev.dealAmountSum, prev.countSum);
            Double changeRate = safeChangeRate(lastAvg, prevAvg);

            result.add(new MonthTop3SalePriceResponse(
                    last.aptId,
                    last.exclusive,
                    last.aptName,
                    last.gugun,
                    last.dong,
                    lastYm,
                    last.countSum,
                    lastAvg,
                    changeRate
            ));
        }

        return result;
    }

    private Long safeAvg(Long sum, Integer count) {
        if (sum == null || count == null || count == 0) return null;
        return sum / count;
    }

    private Double safeChangeRate(Long lastAvg, Long prevAvg) {
        if (lastAvg == null || prevAvg == null || prevAvg == 0L) return null;
        return ((double) (lastAvg - prevAvg) / (double) prevAvg) * 100.0;
    }
}

